package mx.edu.utez.credencialapi.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.edu.utez.credencialapi.servicio.ApiException;
import mx.edu.utez.credencialapi.servicio.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AutenticacionInterceptor implements HandlerInterceptor {

    public static final String ESTUDIANTE_ID = "estudianteId";
    public static final String TOKEN = "token";

    private final AuthService auth;

    public AutenticacionInterceptor(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String encabezado = request.getHeader("Authorization");
        if (encabezado == null || !encabezado.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Falta el token de sesión.");
        }
        String token = encabezado.substring(7).trim();
        request.setAttribute(ESTUDIANTE_ID, auth.estudianteDelToken(token));
        request.setAttribute(TOKEN, token);
        return true;
    }
}
