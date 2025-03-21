package com.insurtech.claim.batch.reader;

import com.insurtech.claim.model.entity.Claim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

public class ClaimReader implements ItemReader<Claim> {

    private static final Logger log = LoggerFactory.getLogger(ClaimReader.class);

    private final JdbcTemplate jdbcTemplate;
    private final Long minValue;
    private final Long maxValue;
    private Iterator<Claim> claimsIterator;

    public ClaimReader(DataSource dataSource, Long minValue, Long maxValue) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.minValue = minValue;
        this.maxValue = maxValue;
        log.info("Inicializado lector de reclamaciones para rango: {} - {}", minValue, maxValue);
    }

    @Override
    public Claim read() throws Exception {
        if (claimsIterator == null) {
            claimsIterator = initializeIterator();
        }

        if (claimsIterator.hasNext()) {
            Claim claim = claimsIterator.next();
            log.debug("Leyendo reclamación: {}", claim.getClaimNumber());
            return claim;
        }

        log.debug("No hay más reclamaciones para leer en este rango");
        return null; // Señala el fin de los datos para este reader
    }

    private Iterator<Claim> initializeIterator() {
        log.info("Cargando reclamaciones para partición: {} - {}", minValue, maxValue);

        // Consulta específica para Oracle
        String query = "SELECT * FROM CLAIMS WHERE id BETWEEN ? AND ? AND " +
                "(status = 'UNDER_REVIEW' OR status = 'SUBMITTED') " +
                "ORDER BY id";

        List<Claim> claims = jdbcTemplate.query(
                query,
                new Object[]{minValue, maxValue},
                new ClaimRowMapper());

        log.info("Cargadas {} reclamaciones para procesamiento", claims.size());
        return claims.iterator();
    }

    /**
     * Mapeador para convertir filas de ResultSet en objetos Claim
     */
    private static class ClaimRowMapper implements RowMapper<Claim> {
        @Override
        public Claim mapRow(ResultSet rs, int rowNum) throws SQLException {
            Claim claim = new Claim();
            claim.setId(rs.getLong("id"));
            claim.setClaimNumber(rs.getString("claim_number"));
            claim.setPolicyId(rs.getLong("policy_id"));
            claim.setPolicyNumber(rs.getString("policy_number"));
            claim.setCustomerId(rs.getLong("customer_id"));
            claim.setCustomerNumber(rs.getString("customer_number"));

            java.sql.Date incidentDate = rs.getDate("incident_date");
            if (incidentDate != null) {
                claim.setIncidentDate(incidentDate.toLocalDate());
            }

            claim.setIncidentDescription(rs.getString("incident_description"));

            String statusStr = rs.getString("status");
            if (statusStr != null) {
                claim.setStatus(Claim.ClaimStatus.valueOf(statusStr));
            }

            String claimTypeStr = rs.getString("claim_type");
            if (claimTypeStr != null) {
                claim.setClaimType(Claim.ClaimType.valueOf(claimTypeStr));
            }

            // Nota: No cargamos todos los campos para evitar carga excesiva
            // El procesador puede cargar más datos si es necesario

            return claim;
        }
    }
}