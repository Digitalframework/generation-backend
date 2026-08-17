package com.image.generation.controller;

import com.image.generation.common.ApiExceptions.NotFoundException;
import com.image.generation.config.SecurityConfig;
import com.image.generation.entity.FurnitureImageContent;
import com.image.generation.entity.Room;
import com.image.generation.impl.ImageService;
import com.image.generation.response.FurnitureImageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} does not pick up {@link SecurityConfig} on its own — it applies
 * Boot's default "authenticate everything" chain instead, under which every test here
 * would fail on a 401 that has nothing to do with what it is checking. Importing the
 * real configuration is also what makes the authorization assertions below mean
 * anything: they exercise the rules the application actually ships.
 */
@WebMvcTest(ImageController.class)
@Import(SecurityConfig.class)
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageService images;

    /**
     * Never called — {@code jwt()} below installs an already-authenticated token
     * rather than presenting one to be decoded. It exists because
     * {@code oauth2ResourceServer} refuses to build a filter chain without a decoder,
     * and the real one would want the JWKS endpoint of a running AuthAndSecurity.
     */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void returnsMetadataAsJson() throws Exception {
        given(images.get(7L)).willReturn(response(7L));

        mockMvc.perform(get("/api/images/7").with(roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hoeffner_1"))
                .andExpect(jsonPath("$.style").value("Scandinavian"))
                .andExpect(jsonPath("$.widthCm").value(180.00))
                .andExpect(jsonPath("$.heightCm").value(90.00))
                .andExpect(jsonPath("$.depthCm").value(40.00))
                .andExpect(jsonPath("$.widthPx").value(1024));
    }

    @Test
    void streamsTheBlobAsPngWithAnETag() throws Exception {
        given(images.content(7L)).willReturn(new FurnitureImageContent("sofa.png", "image/png", "abc123", new byte[]{1, 2, 3}));

        mockMvc.perform(get("/api/images/7/content").with(roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("ETag", "\"abc123\""))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void answers304WhenTheClientAlreadyHasTheImage() throws Exception {
        given(images.content(7L)).willReturn(new FurnitureImageContent("sofa.png", "image/png", "abc123", new byte[]{1, 2, 3}));

        mockMvc.perform(get("/api/images/7/content").with(roles("USER")).header("If-None-Match", "\"abc123\""))
                .andExpect(status().isNotModified());
    }

    @Test
    void answers404ForAnUnknownImage() throws Exception {
        given(images.get(99L)).willThrow(NotFoundException.of("Image", 99L));

        mockMvc.perform(get("/api/images/99").with(roles("USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Image 99 does not exist"));
    }

    /**
     * Reading is no longer open to the world. Both of these used to answer 200, so the
     * pair is here to keep the change from being undone by accident.
     */
    @Test
    void refusesAReadFromAnUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/images"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/images/7"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/images/7/content"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(images);
    }

    @Test
    void letsAPlainUserBrowseTheCatalogue() throws Exception {
        given(images.search(eq(null), eq(null), eq(null), any())).willReturn(new PageImpl<>(List.of(response(7L))));

        mockMvc.perform(get("/api/images").with(roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7));
    }

    @Test
    void rejectsAnUploadWithoutRoomCategoryAndStyle() throws Exception {
        mockMvc.perform(multipart("/api/images").with(admin())
                        .file(new MockMultipartFile("file", "Hoeffner_1_180x90x40.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refusesAnUploadFromAnUnauthenticatedCaller() throws Exception {
        mockMvc.perform(multipart("/api/images")
                        .file(new MockMultipartFile("file", "Hoeffner_1_180x90x40.png", "image/png", new byte[]{1, 2, 3}))
                        .param("room", "livingroom")
                        .param("category", "armchair")
                        .param("style", "Scandinavian"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(images);
    }

    /**
     * The case the whole arrangement exists for: a real, signed-in user who simply is
     * not an admin. 403 rather than 401 — they are recognised, just not permitted.
     */
    @Test
    void refusesAnUploadFromANonAdmin() throws Exception {
        mockMvc.perform(multipart("/api/images").with(roles("USER"))
                        .file(new MockMultipartFile("file", "Hoeffner_1_180x90x40.png", "image/png", new byte[]{1, 2, 3}))
                        .param("room", "livingroom")
                        .param("category", "armchair")
                        .param("style", "Scandinavian"))
                .andExpect(status().isForbidden());

        // The check has to happen before the upload is acted on, not after.
        verifyNoInteractions(images);
    }

    @Test
    void refusesADeleteFromANonAdmin() throws Exception {
        mockMvc.perform(delete("/api/images/7").with(roles("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(images);
    }

    @Test
    void letsAnAdminDelete() throws Exception {
        mockMvc.perform(delete("/api/images/7").with(admin()))
                .andExpect(status().isNoContent());

        verify(images).delete(7L);
    }

    @Test
    void acceptsAnUploadWithRoomCategoryAndStyleAsFormFields() throws Exception {
        given(images.create(argThat(form -> form.room() == Room.LIVING_ROOM
                && "armchair".equals(form.category())
                && "Scandinavian".equals(form.style())
                && form.file() != null)))
                .willReturn(response(7L));

        mockMvc.perform(multipart("/api/images").with(admin())
                        .file(new MockMultipartFile("file", "Hoeffner_1_180x90x40.png", "image/png", new byte[]{1, 2, 3}))
                        .param("room", "livingroom")
                        .param("category", "armchair")
                        .param("style", "Scandinavian"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/images/7"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void namesTheOffendingFieldWhenTheRoomIsUnknown() throws Exception {
        mockMvc.perform(multipart("/api/images").with(admin())
                        .file(new MockMultipartFile("file", "Hoeffner_1_180x90x40.png", "image/png", new byte[]{1, 2, 3}))
                        .param("room", "wintergarden")
                        .param("category", "armchair")
                        .param("style", "Modern"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Unknown room 'wintergarden'")));
    }

    /**
     * A caller carrying the roles the token would have. The {@code ROLE_} prefix is
     * spelled out because these stand in for what SecurityConfig's converter has
     * already produced from the bare {@code roles} claim, not for the claim itself.
     */
    private static JwtRequestPostProcessor roles(String... roles) {
        return jwt().authorities(Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toArray(GrantedAuthority[]::new));
    }

    private static JwtRequestPostProcessor admin() {
        return roles("USER", "ADMIN");
    }

    private static FurnitureImageResponse response(long id) {
        return new FurnitureImageResponse(id, 1L, "living-room-sofa", Room.LIVING_ROOM, "Sofa",
                "Hoeffner_1", "Hoeffner_1_180x90x40.png", "image/png", 3L, 1024, 1024, "abc123",
                new BigDecimal("180.00"), new BigDecimal("90.00"), new BigDecimal("40.00"),
                "Scandinavian", Instant.EPOCH, Instant.EPOCH);
    }
}
