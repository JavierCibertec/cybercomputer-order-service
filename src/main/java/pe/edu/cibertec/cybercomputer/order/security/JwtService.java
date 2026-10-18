package pe.edu.cibertec.cybercomputer.order.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class JwtService {
    private final JwtParser parser;

    public JwtService(@Value("${app.jwt.secret-base64}") String secret,
                      @Value("${app.jwt.issuer}") String issuer) {
        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        parser = Jwts.parser().verifyWith(key).requireIssuer(issuer).build();
    }

    public UsuarioAutenticado validar(String token) {
        var firmado = parser.parseSignedClaims(token);
        var claims = firmado.getPayload();
        if (!"HS256".equals(firmado.getHeader().getAlgorithm())
                || claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getExpiration() == null || claims.getIssuedAt() == null) {
            throw new JwtException("Token incompleto o algoritmo no admitido");
        }
        Object id = claims.get("idusuario");
        Object roles = claims.get("authorities");
        if (!(id instanceof Number) || !(roles instanceof List<?> lista)
                || lista.stream().anyMatch(r -> !(r instanceof String s) || s.isBlank())) {
            throw new JwtException("Identidad del token invalida");
        }
        final int idusuario;
        try {
            idusuario = new BigDecimal(id.toString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new JwtException("Identificador del token invalido");
        }
        if (idusuario <= 0) {
            throw new JwtException("Identificador del token invalido");
        }
        return new UsuarioAutenticado(idusuario, claims.getSubject(),
                lista.stream().map(String.class::cast).distinct().toList());
    }
}
