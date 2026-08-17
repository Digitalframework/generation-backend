alter table furniture_image
    add column name varchar(128);

-- Existing rows predate the naming convention: fall back to the file name.
update furniture_image
set name = regexp_replace(file_name, '\.[^.]*$', '')
where name is null;

alter table furniture_image
    alter column name set not null;

create index idx_image_name on furniture_image (name);
