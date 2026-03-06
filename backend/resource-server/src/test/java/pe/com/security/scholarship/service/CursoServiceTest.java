package pe.com.security.scholarship.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.enums.DiaSemana;
import pe.com.security.scholarship.domain.enums.ModalidadCurso;
import pe.com.security.scholarship.dto.request.RegisterCursoRequest;
import pe.com.security.scholarship.dto.request.RegisterHorarioSeccionRequest;
import pe.com.security.scholarship.dto.request.RegisterSeccionRequest;
import pe.com.security.scholarship.dto.response.RegisteredCursoResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.repository.CursoRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private SeccionService seccionService;

    @InjectMocks
    private CursoService cursoService;

    @Test
    void register_ShouldSucceed_WhenCursoHasSecciones() {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS101");
        request.setNombre("Introducción a la Programación");
        request.setModalidadCurso(ModalidadCurso.ONLINE);

        RegisterHorarioSeccionRequest h1 = new RegisterHorarioSeccionRequest();
        h1.setDiaSemana(DiaSemana.LUNES);
        h1.setHoraInicio(LocalTime.of(8, 0));
        h1.setHoraFin(LocalTime.of(10, 0));

        RegisterSeccionRequest s1 = new RegisterSeccionRequest();
        s1.setFechaInicio(LocalDate.now().plusDays(1));
        s1.setVacantesDisponibles(20);
        s1.setHorarios(Collections.singletonList(h1));

        RegisterSeccionRequest s2 = new RegisterSeccionRequest();
        s2.setFechaInicio(LocalDate.now().plusDays(2));
        s2.setVacantesDisponibles(25);
        s2.setHorarios(Collections.singletonList(h1));

        request.setSecciones(Arrays.asList(s1, s2));

        when(cursoRepository.existsByCodigo(request.getCodigo())).thenReturn(false);
        when(cursoRepository.save(any(Curso.class))).thenAnswer(invocation -> {
            Curso c = invocation.getArgument(0);
            c.setId(1);
            return c;
        });

        // Act
        RegisteredCursoResponse response = cursoService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getSecciones()).hasSize(2);

        verify(seccionService, times(2)).horariosValidos(anyList());
        verify(cursoRepository).save(argThat(curso -> 
            curso.getSecciones() != null && 
            curso.getSecciones().size() == 2 &&
            curso.getSecciones().get(0).getHorarios() != null &&
            !curso.getSecciones().get(0).getHorarios().isEmpty()
        ));
    }

    @Test
    void register_ShouldSucceed_WhenCursoHasNoSecciones() {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS102");
        request.setNombre("Algoritmos");
        request.setModalidadCurso(ModalidadCurso.PRESENCIAL);
        request.setSecciones(null);

        when(cursoRepository.existsByCodigo(request.getCodigo())).thenReturn(false);
        when(cursoRepository.save(any(Curso.class))).thenAnswer(invocation -> {
            Curso c = invocation.getArgument(0);
            c.setId(2);
            c.setSecciones(Collections.emptyList());
            return c;
        });

        // Act
        RegisteredCursoResponse response = cursoService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2);
        assertThat(response.getSecciones()).isEmpty();

        verify(seccionService, never()).horariosValidos(anyList());
        verify(cursoRepository).save(any(Curso.class));
    }

    @Test
    void register_ShouldThrowBadRequest_WhenCodigoExists() {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS101");
        request.setNombre("Curso Duplicado");
        request.setModalidadCurso(ModalidadCurso.ONLINE);

        when(cursoRepository.existsByCodigo(request.getCodigo())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> cursoService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Ya existe un curso con el código ingresado");

        verify(cursoRepository, never()).save(any());
        verify(seccionService, never()).horariosValidos(anyList());
    }

    @Test
    void register_ShouldPropagateException_WhenHorariosAreInvalid() {
        // Arrange
        RegisterCursoRequest request = new RegisterCursoRequest();
        request.setCodigo("CS103");
        request.setNombre("Curso Invalido");
        request.setModalidadCurso(ModalidadCurso.ONLINE);

        RegisterHorarioSeccionRequest h1 = new RegisterHorarioSeccionRequest();
        h1.setDiaSemana(DiaSemana.LUNES);
        h1.setHoraInicio(LocalTime.of(8, 0));
        h1.setHoraFin(LocalTime.of(10, 0));
        
        RegisterSeccionRequest s1 = new RegisterSeccionRequest();
        s1.setHorarios(Collections.singletonList(h1));
        
        request.setSecciones(Collections.singletonList(s1));

        when(cursoRepository.existsByCodigo(request.getCodigo())).thenReturn(false);
        
        doThrow(new BadRequestException("Cruce de horarios detectado"))
                .when(seccionService).horariosValidos(anyList());

        // Act & Assert
        assertThatThrownBy(() -> cursoService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cruce de horarios detectado");

        verify(cursoRepository, never()).save(any());
    }
}
