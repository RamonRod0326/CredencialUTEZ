package mx.edu.utez.credencialapi.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FotoService {

    private final Path carpeta;

    public FotoService(@Value("${app.fotos.carpeta}") String carpeta) throws IOException {
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
        Files.createDirectories(this.carpeta);
    }

    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se recibió ninguna foto.");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El archivo debe ser una imagen.");
        }
        String nombre = UUID.randomUUID() + ".jpg";
        try (InputStream entrada = archivo.getInputStream()) {
            Files.copy(entrada, carpeta.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la foto.");
        }
        return nombre;
    }

    public void borrar(String nombre) {
        if (nombre == null) return;
        try {
            Files.deleteIfExists(resolver(nombre));
        } catch (IOException ignored) {
        }
    }

    public Resource cargar(String nombre) {
        try {
            Resource recurso = new UrlResource(resolver(nombre).toUri());
            if (!recurso.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Foto no encontrada.");
            }
            return recurso;
        } catch (MalformedURLException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Foto no encontrada.");
        }
    }

    private Path resolver(String nombre) {
        Path ruta = carpeta.resolve(nombre).normalize();
        if (!carpeta.equals(ruta.getParent())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nombre de archivo no válido.");
        }
        return ruta;
    }
}
