CREATE TABLE members (
    member_id BIGINT IDENTITY(1,1) NOT NULL,
    email NVARCHAR(320) NOT NULL,
    username NVARCHAR(50) NOT NULL,
    password_hash NVARCHAR(100) NOT NULL,
    display_name NVARCHAR(100) NOT NULL,
    phone NVARCHAR(30) NOT NULL,
    role NVARCHAR(20) NOT NULL CONSTRAINT df_members_role DEFAULT 'MEMBER',
    account_status NVARCHAR(20) NOT NULL CONSTRAINT df_members_status DEFAULT 'ACTIVE',
    must_change_password BIT NOT NULL CONSTRAINT df_members_must_change_password DEFAULT 0,
    created_at DATETIME2(6) NOT NULL CONSTRAINT df_members_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT df_members_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_members PRIMARY KEY (member_id),
    CONSTRAINT ck_members_role CHECK (role IN ('MEMBER', 'ADMIN')),
    CONSTRAINT ck_members_status CHECK (account_status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX ux_members_email ON members (email);
CREATE UNIQUE INDEX ux_members_username ON members (username);
