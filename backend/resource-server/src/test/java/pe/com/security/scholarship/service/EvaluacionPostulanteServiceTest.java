package pe.com.security.scholarship.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.security.scholarship.domain.entity.Convocatoria;
import pe.com.security.scholarship.domain.enums.ModoEvaluacion;
import pe.com.security.scholarship.dto.projection.PostulanteEvaluacionProjection;
import pe.com.security.scholarship.repository.ConvocatoriaRepository;
import pe.com.security.scholarship.repository.PostulacionRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluacionPostulanteServiceTest {

    @Mock
    private ConvocatoriaRepository convocatoriaRepository;

    @Mock
    private PostulacionRepository postulacionRepository;

    @InjectMocks
    private EvaluacionPostulanteService evaluacionPostulanteService;

    @Test
    void evaluarPostulantes_CasoExitoso_ModoMixto() {
        // Arrange
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(1);
        convocatoria.setModoEvaluacion(ModoEvaluacion.MIXTO);

        when(convocatoriaRepository.getUltimaConvocatoriaCerrada()).thenReturn(Optional.of(convocatoria));

        List<Integer> idsPostulaciones = Arrays.asList(101, 102);
        when(postulacionRepository.findIdsByConvocatoria(1)).thenReturn(idsPostulaciones);

        PostulanteEvaluacionProjection p1 = mock(PostulanteEvaluacionProjection.class);
        when(p1.getIdPostulacion()).thenReturn(101);
        when(p1.getPromedioPonderado()).thenReturn(18.0);
        when(p1.getValorEvalSocio()).thenReturn(20); // Integer

        PostulanteEvaluacionProjection p2 = mock(PostulanteEvaluacionProjection.class);
        when(p2.getIdPostulacion()).thenReturn(102);
        when(p2.getPromedioPonderado()).thenReturn(14.0);
        when(p2.getValorEvalSocio()).thenReturn(16); // Integer

        List<PostulanteEvaluacionProjection> evaluaciones = Arrays.asList(p1, p2);
        when(postulacionRepository.getDatosEvaluacion(idsPostulaciones)).thenReturn(evaluaciones);

        when(postulacionRepository.actualizarPromedioGeneralPostulaciones(1)).thenReturn(2);

        // Act
        int result = evaluacionPostulanteService.evaluarPostulantes();

        // Assert
        assertEquals(2, result);

        // Verificación de cálculos
        // P1: (18.0 + 20) / 2.0 = 19.0
        verify(postulacionRepository).actualizarPromedioGeneral(eq(101), eq(19.0));

        // P2: (14.0 + 16) / 2.0 = 15.0
        verify(postulacionRepository).actualizarPromedioGeneral(eq(102), eq(15.0));

        verify(postulacionRepository).actualizarPromedioGeneralPostulaciones(1);
    }

    @Test
    void evaluarPostulantes_CasoDatosNulos_ModoMixto() {
        // 1. ARRANGE
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(2);
        convocatoria.setModoEvaluacion(ModoEvaluacion.MIXTO);

        when(convocatoriaRepository.getUltimaConvocatoriaCerrada()).thenReturn(Optional.of(convocatoria));

        List<Integer> ids = Arrays.asList(201, 202);
        when(postulacionRepository.findIdsByConvocatoria(2)).thenReturn(ids);

        // Postulante 201: Datos nulos (Provocará el 'continue')
        PostulanteEvaluacionProjection p1 = mock(PostulanteEvaluacionProjection.class);
        when(p1.getIdPostulacion()).thenReturn(201);
        when(p1.getPromedioPonderado()).thenReturn(null);

        // Postulante 202: Datos completos
        PostulanteEvaluacionProjection p2 = mock(PostulanteEvaluacionProjection.class);
        when(p2.getIdPostulacion()).thenReturn(202);
        when(p2.getPromedioPonderado()).thenReturn(16.0);
        when(p2.getValorEvalSocio()).thenReturn(14);

        when(postulacionRepository.getDatosEvaluacion(ids)).thenReturn(Arrays.asList(p1, p2));

        // IMPORTANTE: Solo mockeamos esto si el flujo realmente llega al final del método.
        // Como el postulante 202 es válido, el bucle NO se rompe y el método SÍ llega al return final.
        when(postulacionRepository.actualizarPromedioGeneralPostulaciones(2)).thenReturn(1);

        // 2. ACT
        int result = evaluacionPostulanteService.evaluarPostulantes();

        // 3. ASSERT
        assertEquals(1, result);

        // Verificamos que para el 201 NUNCA se llamó al update (por ser nulo)
        verify(postulacionRepository, never()).actualizarPromedioGeneral(eq(201), anyDouble());

        // Verificamos que para el 202 SÍ se llamó con el cálculo correcto
        verify(postulacionRepository, times(1)).actualizarPromedioGeneral(eq(202), eq(15.0));
    }

    @Test
    void evaluarPostulantes_CasoSinConvocatoria() {
        // Arrange
        when(convocatoriaRepository.getUltimaConvocatoriaCerrada()).thenReturn(Optional.empty());

        // Act
        int result = evaluacionPostulanteService.evaluarPostulantes();

        // Assert
        assertEquals(0, result);
        verify(postulacionRepository, never()).findIdsByConvocatoria(anyInt());
        verify(postulacionRepository, never()).getDatosEvaluacion(any());
        verify(postulacionRepository, never()).actualizarPromedioGeneralPostulaciones(anyInt());
    }

    @Test
    void evaluarPostulantes_ModoPromedioPonderado() {
        // Arrange
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(3);
        convocatoria.setModoEvaluacion(ModoEvaluacion.PROMEDIO_PONDERADO);

        when(convocatoriaRepository.getUltimaConvocatoriaCerrada()).thenReturn(Optional.of(convocatoria));

        List<Integer> ids = Collections.singletonList(301);
        when(postulacionRepository.findIdsByConvocatoria(3)).thenReturn(ids);

        PostulanteEvaluacionProjection p1 = mock(PostulanteEvaluacionProjection.class);
        when(p1.getIdPostulacion()).thenReturn(301);
        when(p1.getPromedioPonderado()).thenReturn(17.5);

        when(postulacionRepository.getDatosEvaluacion(ids)).thenReturn(Collections.singletonList(p1));
        when(postulacionRepository.actualizarPromedioGeneralPostulaciones(3)).thenReturn(1);

        // Act
        int result = evaluacionPostulanteService.evaluarPostulantes();

        // Assert
        assertEquals(1, result);
        verify(postulacionRepository).actualizarPromedioGeneral(eq(301), eq(17.5));
    }

    @Test
    void evaluarPostulantes_ModoSocioeconomico() {
        // Arrange
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(4);
        convocatoria.setModoEvaluacion(ModoEvaluacion.SOCIOECONOMICO);

        when(convocatoriaRepository.getUltimaConvocatoriaCerrada()).thenReturn(Optional.of(convocatoria));

        List<Integer> ids = Collections.singletonList(401);
        when(postulacionRepository.findIdsByConvocatoria(4)).thenReturn(ids);

        PostulanteEvaluacionProjection p1 = mock(PostulanteEvaluacionProjection.class);
        when(p1.getIdPostulacion()).thenReturn(401);
        when(p1.getValorEvalSocio()).thenReturn(20); // Integer

        when(postulacionRepository.getDatosEvaluacion(ids)).thenReturn(Collections.singletonList(p1));
        when(postulacionRepository.actualizarPromedioGeneralPostulaciones(4)).thenReturn(1);

        // Act
        int result = evaluacionPostulanteService.evaluarPostulantes();

        // Assert
        assertEquals(1, result);
        verify(postulacionRepository).actualizarPromedioGeneral(eq(401), eq(20.0)); // Verifica conversión a Double
    }

    @Test
    void evaluarPostulantes_DeberiaContinuar_CuandoUnUpdateFalla() {
        // Arrange
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(5);
        convocatoria.setModoEvaluacion(ModoEvaluacion.PROMEDIO_PONDERADO);

        when(convocatoriaRepository.getUltimaConvocatoriaCerrada()).thenReturn(Optional.of(convocatoria));

        List<Integer> ids = Arrays.asList(501, 502);
        when(postulacionRepository.findIdsByConvocatoria(5)).thenReturn(ids);

        PostulanteEvaluacionProjection p1 = mock(PostulanteEvaluacionProjection.class);
        when(p1.getIdPostulacion()).thenReturn(501);
        when(p1.getPromedioPonderado()).thenReturn(15.0);

        PostulanteEvaluacionProjection p2 = mock(PostulanteEvaluacionProjection.class);
        when(p2.getIdPostulacion()).thenReturn(502);
        when(p2.getPromedioPonderado()).thenReturn(18.0);

        when(postulacionRepository.getDatosEvaluacion(ids)).thenReturn(Arrays.asList(p1, p2));

        // Simular excepción en el primer update
        doThrow(new RuntimeException("Error de BD")).when(postulacionRepository).actualizarPromedioGeneral(eq(501), anyDouble());
        
        when(postulacionRepository.actualizarPromedioGeneralPostulaciones(5)).thenReturn(1);

        // Act
        int result = evaluacionPostulanteService.evaluarPostulantes();

        // Assert
        assertEquals(1, result);
        verify(postulacionRepository).actualizarPromedioGeneral(eq(501), eq(15.0)); // Se intentó llamar
        verify(postulacionRepository).actualizarPromedioGeneral(eq(502), eq(18.0)); // Se llamó exitosamente al segundo
    }

    @Test
    void evaluarPostulantes_CasoSocioeconomicoNulo() {
        // Arrange
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(6);
        convocatoria.setModoEvaluacion(ModoEvaluacion.SOCIOECONOMICO);

        when(convocatoriaRepository.getUltimaConvocatoriaCerrada()).thenReturn(Optional.of(convocatoria));

        List<Integer> ids = Collections.singletonList(601);
        when(postulacionRepository.findIdsByConvocatoria(6)).thenReturn(ids);

        PostulanteEvaluacionProjection p1 = mock(PostulanteEvaluacionProjection.class);
        when(p1.getIdPostulacion()).thenReturn(601);
        when(p1.getValorEvalSocio()).thenReturn(null); // Nulo

        when(postulacionRepository.getDatosEvaluacion(ids)).thenReturn(Collections.singletonList(p1));
        when(postulacionRepository.actualizarPromedioGeneralPostulaciones(6)).thenReturn(0);

        // Act
        int result = evaluacionPostulanteService.evaluarPostulantes();

        // Assert
        assertEquals(0, result);
        verify(postulacionRepository, never()).actualizarPromedioGeneral(eq(601), anyDouble());
    }

    @Test
    void evaluarPostulantes_ListaProyeccionesVacia() {
        // Arrange
        Convocatoria convocatoria = new Convocatoria();
        convocatoria.setId(7);
        convocatoria.setModoEvaluacion(ModoEvaluacion.PROMEDIO_PONDERADO);

        when(convocatoriaRepository.getUltimaConvocatoriaCerrada()).thenReturn(Optional.of(convocatoria));

        List<Integer> ids = Collections.singletonList(701);
        when(postulacionRepository.findIdsByConvocatoria(7)).thenReturn(ids);

        // Caso borde: hay IDs pero getDatosEvaluacion retorna vacío (inconsistencia de datos o filtro)
        when(postulacionRepository.getDatosEvaluacion(ids)).thenReturn(Collections.emptyList());
        
        when(postulacionRepository.actualizarPromedioGeneralPostulaciones(7)).thenReturn(0);

        // Act
        int result = evaluacionPostulanteService.evaluarPostulantes();

        // Assert
        assertEquals(0, result);
        verify(postulacionRepository, never()).actualizarPromedioGeneral(anyInt(), anyDouble());
    }
}
