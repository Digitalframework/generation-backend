# generation-backend

Spring Boot 4.1 / Java 25 CRUD backend for categorized furniture renderings.
Images are 1024x1024 PNGs stored as `bytea` blobs in PostgreSQL, together with the
real-world dimensions (width / depth / height in cm) and the style they depict.

## Data model

Two tables, both owned by Flyway (`src/main/resources/db/migration`):

| Table                | Purpose                                                                 |
|----------------------|-------------------------------------------------------------------------|
| `furniture_category` | one room + furniture kind, e.g. `LIVING_ROOM` + `Sofa`, with a unique `slug` |
| `furniture_image`    | the PNG blob + `name` + `width_cm` / `height_cm` / `depth_cm` / `style`, FK to the category |

`V2` seeds the nine living-room categories (armchair, sofa, sideboard, highboard,
lowboard, coffee table, media wall, ottoman, shelves). Other rooms are created
through the API — `Room` is an enum (`LIVING_ROOM`, `BEDROOM`, `DINING_ROOM`,
`KITCHEN`, `BATHROOM`, `HOME_OFFICE`, `HALLWAY`, `KIDS_ROOM`, `OUTDOOR`), the
furniture kind is free text so you can add new ones without a migration.

The blob column is never selected by the list/detail queries — those go through a
DTO projection (`FurnitureImageResponse`), so listing 100 images does not drag
100 × ~1.5 MB through the JVM. Only `GET /{id}/content` reads the bytes.

## Running

```bash
cp .env.example .env          # then set DB_PASSWORD
createdb -U postgres GenerationServer
./mvnw spring-boot:run        # http://localhost:8080
```

Defaults are `localhost:5432/GenerationServer`, user `postgres`; override with
`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` in `.env` or as environment variables.
Flyway creates the schema on first start; Hibernate runs with `ddl-auto=validate`.

## Swagger UI

**<http://localhost:8080/swagger-ui.html>** — the whole API in the browser, no curl needed.
The schema itself is at `/v3/api-docs`.

To upload a picture: open **POST /api/images** → *Try it out* → pick the PNG file,
choose the **room** from the dropdown, type the **category** and the **style** → *Execute*.

    file      Hoeffner_1_180x90x40.png
    room      LIVING_ROOM
    category  Armchair
    style     Modern

The furniture name and the measurements are not fields — they come from the file name.

## API

### Categories — `/api/categories`

Reading the tree is open; changing it takes an ADMIN token.

| Method | Path                        | Who       | Notes                                                |
|--------|-----------------------------|-----------|-------------------------------------------------------|
| GET    | `/api/categories`           | anyone    | paged; `?room=LIVING_ROOM`, `?page=&size=&sort=`       |
| GET    | `/api/categories/{id}`      | anyone    |                                                        |
| GET    | `/api/categories/slug/{slug}` | anyone  | e.g. `living-room-coffee-table`                        |
| GET    | `/api/categories/{id}/images` | anyone  | paged image metadata, `?style=`                        |
| POST   | `/api/categories`           | ADMIN     | JSON body; slug derived from room + name when omitted  |
| PUT    | `/api/categories/{id}`      | ADMIN     | full update                                            |
| DELETE | `/api/categories/{id}`      | ADMIN     | 409 while the category still holds images              |

```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"room":"BEDROOM","name":"Wardrobe","description":"free-standing"}'
```

### Images — `/api/images`

Every one of these needs a token — there is no anonymous access to the pictures.

| Method | Path                       | Who          | Notes                                             |
|--------|----------------------------|--------------|---------------------------------------------------|
| GET    | `/api/images`              | USER / ADMIN | paged metadata; `?categoryId=`, `?room=`, `?style=` |
| GET    | `/api/images/{id}`         | USER / ADMIN | metadata only                                      |
| GET    | `/api/images/{id}/content` | USER / ADMIN | the PNG itself, with `ETag` + one-year `Cache-Control` |
| POST   | `/api/images`              | ADMIN        | `multipart/form-data` upload: `file`, `room`, `category`, `style` |
| PUT    | `/api/images/{id}`         | ADMIN        | JSON, corrects name / category / measurements / style by hand |
| PUT    | `/api/images/{id}/content` | ADMIN        | `multipart/form-data`, replaces the PNG (name and measurements are re-read from the new file name) |
| DELETE | `/api/images/{id}`         | ADMIN        |                                                    |

#### The file name carries the name and the measurements

`Hoeffner_1_180x90x40.png` means: name `Hoeffner_1`, **180 cm wide, 90 cm high,
40 cm deep** — `<name>_<width>x<height>x<depth>.png`, in centimetres. So an upload
only needs the category and the style as form fields:

The same request outside Swagger UI:

```bash
curl -X POST http://localhost:8080/api/images \
  -F "file=@Hoeffner_1_180x90x40.png;type=image/png" \
  -F "room=livingroom" -F "category=armchair" -F "style=Modern"
```

`room` is matched leniently (`livingroom`, `living room`, `living-room`, `LIVING_ROOM`)
and `category` case-insensitively against the kinds that exist in that room; an unknown
one answers 404 listing what *is* there.

The name may contain underscores and even an `x` — the *last* `WxHxD` group wins,
so `Hoeffner_Sofa_2x3_Sitzer_200.5x85x95.png` parses as name
`Hoeffner_Sofa_2x3_Sitzer`, 200.5 × 85 × 95. Also tolerated: upper-case `X`, `×`,
spaces around the separators, a trailing `cm`, a comma as decimal mark, and a dash
instead of the underscore in front of the measurements. A file name the parser
cannot read is rejected with 400 and a message naming the expected format; use
`PUT /api/images/{id}` afterwards if a value needs correcting by hand.

Uploads are also rejected (400) unless the bytes really are a PNG (magic-number
check, not just the declared content type) and exactly 1024x1024. Re-uploading
identical bytes is rejected with 409 — the SHA-256 of every blob is unique.

### Rendering the picture — server side, not in the client

`GET /api/images/{id}/content` streams the blob back with `Content-Type: image/png`,
so the client needs no decoding at all. It does need the token, though, and an
`<img src>` cannot carry one — fetch the bytes and hand the tag an object URL:

```js
const res = await fetch("http://localhost:8080/api/images/42/content", {
  headers: { Authorization: `Bearer ${accessToken}` },
});
img.src = URL.createObjectURL(await res.blob());
```

The `ETag` is the blob's SHA-256, so a browser re-requesting an unchanged image
gets a `304 Not Modified` instead of the payload. `Cache-Control` is `private`, so
the browser keeps the picture but a shared proxy will not. Each metadata response
also carries `contentUrl` pointing at that endpoint. Base64-ing blobs into JSON is
the alternative, and it is worse: ~33 % larger and uncacheable.

Errors come back as RFC 9457 problem details (`status`, `title`, `detail`).

## Tests

```bash
./mvnw test
```

`FurnitureFileNameTest`, `PngUploadTest` and `SlugifyTest` are plain unit tests; `ImageControllerTest` is a
`@WebMvcTest` slice. `GenerationBackendApplicationTests` boots the whole context and
therefore needs a reachable PostgreSQL.
