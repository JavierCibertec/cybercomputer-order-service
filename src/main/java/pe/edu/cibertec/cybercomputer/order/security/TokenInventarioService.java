package pe.edu.cibertec.cybercomputer.order.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class TokenInventarioService {
    private final SecretKey key;
    private final String issuer;

    public TokenInventarioService(@Value("${app.jwt.secret-base64}") String secret,
                                  @Value("${app.jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
    }

    public String authorization(Integer idusuario) {
        Instant now = Instant.now();
        return "Bearer " + Jwts.builder().issuer(issuer).subject("order-service")
                .claim("idusuario", idusuario)
                .claim("authorities", List.of("ROLE_ORDER_SERVICE"))
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }
}
