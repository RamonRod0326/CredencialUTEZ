package mx.edu.utez.credencialapi.web;

import jakarta.validation.Valid;
import mx.edu.utez.credencialapi.config.AutenticacionInterceptor;
import mx.edu.utez.credencialapi.dto.EstudianteResponse;
import mx.edu.utez.credencialapi.dto.PerfilRequest;
import mx.edu.utez.credencialapi.servicio.EstudianteService;
import mx.edu.utez.credencialapi.servicio.FotoService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class EstudianteController {

    private final EstudianteService estudiantes;
    private final FotoService fotos;

    public EstudianteController(EstudianteService estudiantes, FotoService fotos) {
        this.estudiantes = estudiantes;
        this.fotos = fotos;
    }

    @GetMapping("/estudiantes/yo")
    public EstudianteResponse yo(@RequestAttribute(AutenticacionInterceptor.ESTUDIANTE_ID) Long id) {
        return estudiantes.obtener(id);
    }

    @PutMapping("/estudiantes/yo")
    public EstudianteResponse actualizar(@RequestAttribute(AutenticacionInterceptor.ESTUDIANTE_ID) Long id,
                                         @Valid @RequestBody PerfilRequest req) {
        return estudiantes.actualizar(id, req);
    }

    @PutMapping(value = "/estudiantes/yo/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EstudianteResponse cambiarFoto(@RequestAttribute(AutenticacionInterceptor.ESTUDIANTE_ID) Long id,
                                          @RequestPart("foto") MultipartFile foto) {
        return estudiantes.cambiarFoto(id, foto);
    }

    @DeleteMapping("/estudiantes/yo/foto")
    public EstudianteResponse eliminarFoto(@RequestAttribute(AutenticacionInterceptor.ESTUDIANTE_ID) Long id) {
        return estudiantes.eliminarFoto(id);
    }

    @DeleteMapping("/estudiantes/yo")
    public ResponseEntity<Void> eliminarCuenta(@RequestAttribute(AutenticacionInterceptor.ESTUDIANTE_ID) Long id) {
        estudiantes.eliminarCuenta(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fotos/{nombre}")
    public ResponseEntity<Resource> foto(@PathVariable String nombre) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                .body(fotos.cargar(nombre));
    }
}
