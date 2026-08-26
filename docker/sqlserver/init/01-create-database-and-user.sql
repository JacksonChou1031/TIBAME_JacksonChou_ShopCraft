IF DB_ID(N'ecommerce') IS NULL
BEGIN
    CREATE DATABASE ecommerce;
END;
GO

USE ecommerce;
GO

IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'ecommerce_app')
BEGIN
    CREATE LOGIN ecommerce_app WITH PASSWORD = '$(APP_PASSWORD)';
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'ecommerce_app')
BEGIN
    CREATE USER ecommerce_app FOR LOGIN ecommerce_app;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    INNER JOIN sys.database_principals role_principal ON role_principal.principal_id = drm.role_principal_id
    INNER JOIN sys.database_principals member_principal ON member_principal.principal_id = drm.member_principal_id
    WHERE role_principal.name = N'db_datareader'
      AND member_principal.name = N'ecommerce_app'
)
BEGIN
    ALTER ROLE db_datareader ADD MEMBER ecommerce_app;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.database_role_members drm
    INNER JOIN sys.database_principals role_principal ON role_principal.principal_id = drm.role_principal_id
    INNER JOIN sys.database_principals member_principal ON member_principal.principal_id = drm.member_principal_id
    WHERE role_principal.name = N'db_datawriter'
      AND member_principal.name = N'ecommerce_app'
)
BEGIN
ALTER ROLE db_datawriter ADD MEMBER ecommerce_app;
END;

GRANT CREATE TABLE TO ecommerce_app;
GRANT ALTER ON SCHEMA::dbo TO ecommerce_app;
GRANT REFERENCES ON SCHEMA::dbo TO ecommerce_app;
GO
