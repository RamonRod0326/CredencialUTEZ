package mx.edu.utez.credencialapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AutenticacionInterceptor autenticacion;

    public WebConfig(AutenticacionInterceptor autenticacion) {
        this.autenticacion = autenticacion;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacion)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/salud",
                        "/api/auth/verificar",
                        "/api/auth/registro",
                        "/api/auth/login",
                        "/api/fotos/**"
                );
    }
}
