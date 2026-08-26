package com.jackson.ecommerce.member.repository;

import com.jackson.ecommerce.member.domain.AccountStatus;
import com.jackson.ecommerce.member.domain.Member;
import com.jackson.ecommerce.member.domain.MemberRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Repository
public class MemberRepository {

    private static final String SELECT_MEMBER = """
            SELECT member_id, email, username, password_hash, display_name, phone,
                   role, account_status, must_change_password
              FROM members
            """;

    private final JdbcTemplate jdbcTemplate;

    public MemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Member> findById(long id) {
        List<Member> members = jdbcTemplate.query(
                SELECT_MEMBER + " WHERE member_id = ?",
                (rs, rowNum) -> mapMember(rs),
                id);
        return members.stream().findFirst();
    }

    public Optional<Member> findByLogin(String identifier) {
        String normalizedIdentifier = normalize(identifier);
        List<Member> members = jdbcTemplate.query(
                SELECT_MEMBER + " WHERE email = ? OR username = ?",
                (rs, rowNum) -> mapMember(rs),
                normalizedIdentifier,
                normalizedIdentifier);
        return members.stream().findFirst();
    }

    public long insert(String email, String username, String passwordHash, String displayName,
                       String phone, MemberRole role, boolean mustChangePassword) {
        String sql = """
                INSERT INTO members
                    (email, username, password_hash, display_name, phone, role, must_change_password)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, email);
            statement.setString(2, username);
            statement.setString(3, passwordHash);
            statement.setString(4, displayName);
            statement.setString(5, phone);
            statement.setString(6, role.name());
            statement.setBoolean(7, mustChangePassword);
            return statement;
        }, keyHolder);
        Map<String, Object> keys = keyHolder.getKeys();
        Object key = keys.get("MEMBER_ID");
        if (key == null) {
            key = keys.get("member_id");
        }
        if (!(key instanceof Number memberId)) {
            throw new IllegalStateException("Member id was not returned after insert");
        }
        return memberId.longValue();
    }

    public int updatePassword(long memberId, String passwordHash) {
        return jdbcTemplate.update(
                "UPDATE members SET password_hash = ?, must_change_password = 0, updated_at = CURRENT_TIMESTAMP WHERE member_id = ?",
                passwordHash,
                memberId);
    }

    public int updateProfile(long memberId, String displayName, String phone) {
        return jdbcTemplate.update(
                "UPDATE members SET display_name = ?, phone = ?, updated_at = CURRENT_TIMESTAMP WHERE member_id = ?",
                displayName,
                phone,
                memberId);
    }

    private Member mapMember(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Member(
                rs.getLong("member_id"),
                rs.getString("email"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("display_name"),
                rs.getString("phone"),
                MemberRole.valueOf(rs.getString("role")),
                AccountStatus.valueOf(rs.getString("account_status")),
                rs.getBoolean("must_change_password"));
    }

    public static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

}
