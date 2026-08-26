CREATE TABLE app_schema_metadata (
    id BIGINT IDENTITY(1,1) NOT NULL,
    schema_name NVARCHAR(100) NOT NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_app_schema_metadata_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_app_schema_metadata PRIMARY KEY (id)
);

INSERT INTO app_schema_metadata (schema_name)
VALUES ('ecommerce');
