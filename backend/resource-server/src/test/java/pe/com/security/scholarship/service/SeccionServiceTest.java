package pe.com.security.scholarship.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.security.scholarship.domain.entity.Curso;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.domain.enums.DiaSemana;
import pe.com.security.scholarship.dto.request.RegisterHorarioSeccionRequest;
import pe.com.security.scholarship.dto.request.RegisterSeccionRequest;
import pe.com.security.scholarship.dto.request.UpdateVacantesSeccionRequest;
import pe.com.security.scholarship.dto.response.RegisteredSeccionResponse;
import pe.com.security.scholarship.dto.response.UpdatedVacantesSeccionResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.CursoRepository;
import pe.com.security.scholarship.repository.MatriculaRepository;
import pe.com.security.scholarship.repository.SeccionRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeccionServiceTest {

    @Mock
    private SeccionRepository seccionRepository;

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private MatriculaRepository matriculaRepository;

    @InjectMocks
    private SeccionService seccionService;

    // --- Tests para register ---

    @Test
    void register_ShouldSucceed_WhenHorariosAreValidOnDifferentDays() {
        // Arrange
        Integer idCurso = 1;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now().plusDays(1));
        request.setVacantesDisponibles(20);

        RegisterHorarioSeccionRequest h1 = new RegisterHorarioSeccionRequest();
        h1.setDiaSemana(DiaSemana.LUNES);
        h1.setHoraInicio(LocalTime.of(8, 0));
        h1.setHoraFin(LocalTime.of(10, 0));

        RegisterHorarioSeccionRequest h2 = new RegisterHorarioSeccionRequest();
        h2.setDiaSemana(DiaSemana.MARTES);
        h2.setHoraInicio(LocalTime.of(8, 0));
        h2.setHoraFin(LocalTime.of(10, 0));

        request.setHorarios(Arrays.asList(h1, h2));

        Curso curso = new Curso();
        curso.setId(idCurso);

        when(cursoRepository.findById(idCurso)).thenReturn(Optional.of(curso));
        when(seccionRepository.save(any(Seccion.class))).thenAnswer(invocation -> {
            Seccion s = invocation.getArgument(0);
            s.setId(100); // Simular ID generado
            return s;
        });

        // Act
        RegisteredSeccionResponse response = seccionService.register(request, idCurso);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100);
        assertThat(response.getHorarios()).hasSize(2);

        verify(seccionRepository).save(argThat(seccion -> 
            seccion.getHorarios() != null && seccion.getHorarios().size() == 2
        ));
    }

    @Test
    void register_ShouldSucceed_WhenHorariosAreValidOnSameDayWithoutOverlap() {
        // Arrange
        Integer idCurso = 1;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now().plusDays(1));
        request.setVacantesDisponibles(20);

        RegisterHorarioSeccionRequest h1 = new RegisterHorarioSeccionRequest();
        h1.setDiaSemana(DiaSemana.LUNES);
        h1.setHoraInicio(LocalTime.of(8, 0));
        h1.setHoraFin(LocalTime.of(10, 0));

        RegisterHorarioSeccionRequest h2 = new RegisterHorarioSeccionRequest();
        h2.setDiaSemana(DiaSemana.LUNES);
        h2.setHoraInicio(LocalTime.of(11, 0));
        h2.setHoraFin(LocalTime.of(13, 0));

        request.setHorarios(Arrays.asList(h1, h2));

        Curso curso = new Curso();
        curso.setId(idCurso);

        when(cursoRepository.findById(idCurso)).thenReturn(Optional.of(curso));
        when(seccionRepository.save(any(Seccion.class))).thenAnswer(invocation -> {
            Seccion s = invocation.getArgument(0);
            s.setId(101);
            return s;
        });

        // Act
        RegisteredSeccionResponse response = seccionService.register(request, idCurso);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101);
        assertThat(response.getHorarios()).hasSize(2);

        verify(seccionRepository).save(any(Seccion.class));
    }

    @Test
    void register_ShouldThrowBadRequest_WhenHoraFinIsEqualToHoraInicio() {
        // Arrange
        Integer idCurso = 1;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now());
        request.setVacantesDisponibles(10);

        RegisterHorarioSeccionRequest h1 = new RegisterHorarioSeccionRequest();
        h1.setDiaSemana(DiaSemana.LUNES);
        h1.setHoraInicio(LocalTime.of(8, 0));
        h1.setHoraFin(LocalTime.of(8, 0)); // Igual

        request.setHorarios(Collections.singletonList(h1));

        Curso curso = new Curso();
        curso.setId(idCurso);

        when(cursoRepository.findById(idCurso)).thenReturn(Optional.of(curso));

        // Act & Assert
        assertThatThrownBy(() -> seccionService.register(request, idCurso))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("La hora de fin (08:00) debe ser posterior a la de inicio (08:00)");

        verify(seccionRepository, never()).save(any());
    }

    @Test
    void register_ShouldThrowBadRequest_WhenHoraFinIsBeforeHoraInicio() {
        // Arrange
        Integer idCurso = 1;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now());
        request.setVacantesDisponibles(10);

        RegisterHorarioSeccionRequest h1 = new RegisterHorarioSeccionRequest();
        h1.setDiaSemana(DiaSemana.LUNES);
        h1.setHoraInicio(LocalTime.of(10, 0));
        h1.setHoraFin(LocalTime.of(8, 0)); // Anterior

        request.setHorarios(Collections.singletonList(h1));

        Curso curso = new Curso();
        curso.setId(idCurso);

        when(cursoRepository.findById(idCurso)).thenReturn(Optional.of(curso));

        // Act & Assert
        assertThatThrownBy(() -> seccionService.register(request, idCurso))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("La hora de fin (08:00) debe ser posterior a la de inicio (10:00)");

        verify(seccionRepository, never()).save(any());
    }

    @Test
    void register_ShouldThrowBadRequest_WhenTotalOverlap() {
        // Arrange
        Integer idCurso = 1;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now());
        request.setVacantesDisponibles(10);

        // 08:00 - 12:00
        RegisterHorarioSeccionRequest h1 = new RegisterHorarioSeccionRequest();
        h1.setDiaSemana(DiaSemana.LUNES);
        h1.setHoraInicio(LocalTime.of(8, 0));
        h1.setHoraFin(LocalTime.of(12, 0));

        // 09:00 - 10:00 (Contenido en el primero)
        RegisterHorarioSeccionRequest h2 = new RegisterHorarioSeccionRequest();
        h2.setDiaSemana(DiaSemana.LUNES);
        h2.setHoraInicio(LocalTime.of(9, 0));
        h2.setHoraFin(LocalTime.of(10, 0));

        request.setHorarios(Arrays.asList(h1, h2));

        Curso curso = new Curso();
        curso.setId(idCurso);

        when(cursoRepository.findById(idCurso)).thenReturn(Optional.of(curso));

        // Act & Assert
        assertThatThrownBy(() -> seccionService.register(request, idCurso))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cruce el LUNES");

        verify(seccionRepository, never()).save(any());
    }

    @Test
    void register_ShouldThrowBadRequest_WhenPartialOverlap() {
        // Arrange
        Integer idCurso = 1;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now());
        request.setVacantesDisponibles(10);

        // 08:00 - 10:00
        RegisterHorarioSeccionRequest h1 = new RegisterHorarioSeccionRequest();
        h1.setDiaSemana(DiaSemana.LUNES);
        h1.setHoraInicio(LocalTime.of(8, 0));
        h1.setHoraFin(LocalTime.of(10, 0));

        // 09:30 - 11:00 (Empieza antes de que termine el primero)
        RegisterHorarioSeccionRequest h2 = new RegisterHorarioSeccionRequest();
        h2.setDiaSemana(DiaSemana.LUNES);
        h2.setHoraInicio(LocalTime.of(9, 30));
        h2.setHoraFin(LocalTime.of(11, 0));

        request.setHorarios(Arrays.asList(h1, h2));

        Curso curso = new Curso();
        curso.setId(idCurso);

        when(cursoRepository.findById(idCurso)).thenReturn(Optional.of(curso));

        // Act & Assert
        assertThatThrownBy(() -> seccionService.register(request, idCurso))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cruce el LUNES");

        verify(seccionRepository, never()).save(any());
    }

    @Test
    void register_ShouldSucceed_WhenBoundaryCaseNoOverlap() {
        // Arrange
        Integer idCurso = 1;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now());
        request.setVacantesDisponibles(10);

        // 08:00 - 10:00
        RegisterHorarioSeccionRequest h1 = new RegisterHorarioSeccionRequest();
        h1.setDiaSemana(DiaSemana.LUNES);
        h1.setHoraInicio(LocalTime.of(8, 0));
        h1.setHoraFin(LocalTime.of(10, 0));

        // 10:00 - 12:00 (Empieza exactamente cuando termina el anterior)
        RegisterHorarioSeccionRequest h2 = new RegisterHorarioSeccionRequest();
        h2.setDiaSemana(DiaSemana.LUNES);
        h2.setHoraInicio(LocalTime.of(10, 0));
        h2.setHoraFin(LocalTime.of(12, 0));

        request.setHorarios(Arrays.asList(h1, h2));

        Curso curso = new Curso();
        curso.setId(idCurso);

        when(cursoRepository.findById(idCurso)).thenReturn(Optional.of(curso));
        when(seccionRepository.save(any(Seccion.class))).thenAnswer(invocation -> {
            Seccion s = invocation.getArgument(0);
            s.setId(102);
            return s;
        });

        // Act
        RegisteredSeccionResponse response = seccionService.register(request, idCurso);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(102);
        verify(seccionRepository).save(any(Seccion.class));
    }

    @Test
    void register_ShouldThrowNotFound_WhenCursoDoesNotExist() {
        // Arrange
        Integer idCurso = 999;
        RegisterSeccionRequest request = new RegisterSeccionRequest();
        request.setFechaInicio(LocalDate.now());
        request.setVacantesDisponibles(10);
        request.setHorarios(Collections.emptyList());

        when(cursoRepository.findById(idCurso)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> seccionService.register(request, idCurso))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No se encontró curso con el ID ingresado");

        verify(seccionRepository, never()).save(any());
    }

    // --- Tests para updateVacantes (existentes) ---

    @Test
    void updateVacantes_ShouldSucceed_WhenFirstSettingOfVacancies() {
        // Arrange
        Integer idSeccion = 1;
        Integer requestedVacancies = 20;
        UpdateVacantesSeccionRequest request = new UpdateVacantesSeccionRequest();
        request.setIdSeccion(idSeccion);
        request.setCantidadVacantes(requestedVacancies);

        Seccion seccion = new Seccion();
        seccion.setId(idSeccion);
        seccion.setVacantesDisponibles(null); // Primer seteo

        when(seccionRepository.findById(idSeccion)).thenReturn(Optional.of(seccion));
        when(matriculaRepository.matricularPostulantes(eq(idSeccion), eq(requestedVacancies))).thenReturn(5);

        // Act
        UpdatedVacantesSeccionResponse response = seccionService.updateVacantes(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCantidadNuevosMatriculados()).isEqualTo(5);
        assertThat(response.getTotalMatriculados()).isEqualTo(requestedVacancies);
        
        verify(seccionRepository, times(1)).save(seccion);
        // Verifica que se llame con el total solicitado porque vacantesDisponibles era null
        verify(matriculaRepository, times(1)).matricularPostulantes(idSeccion, requestedVacancies);
    }

    @Test
    void updateVacantes_ShouldSucceed_WhenIncreasingVacancies() {
        // Arrange
        Integer idSeccion = 1;
        Integer currentVacancies = 10;
        Integer requestedVacancies = 15;
        Integer expectedIncrement = 5;

        UpdateVacantesSeccionRequest request = new UpdateVacantesSeccionRequest();
        request.setIdSeccion(idSeccion);
        request.setCantidadVacantes(requestedVacancies);

        Seccion seccion = new Seccion();
        seccion.setId(idSeccion);
        seccion.setVacantesDisponibles(currentVacancies);

        when(seccionRepository.findById(idSeccion)).thenReturn(Optional.of(seccion));
        when(matriculaRepository.matricularPostulantes(eq(idSeccion), eq(expectedIncrement))).thenReturn(3);

        // Act
        UpdatedVacantesSeccionResponse response = seccionService.updateVacantes(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCantidadNuevosMatriculados()).isEqualTo(3);
        assertThat(response.getTotalMatriculados()).isEqualTo(requestedVacancies);

        verify(seccionRepository, times(1)).save(seccion);
        // Verifica que se llame con la diferencia (15 - 10 = 5)
        verify(matriculaRepository, times(1)).matricularPostulantes(idSeccion, expectedIncrement);
    }

    @Test
    void updateVacantes_ShouldThrowBadRequest_WhenNewVacanciesAreLessOrEqual() {
        // Arrange
        Integer idSeccion = 1;
        Integer currentVacancies = 20;
        Integer requestedVacancies = 15; // Menor que actual

        UpdateVacantesSeccionRequest request = new UpdateVacantesSeccionRequest();
        request.setIdSeccion(idSeccion);
        request.setCantidadVacantes(requestedVacancies);

        Seccion seccion = new Seccion();
        seccion.setId(idSeccion);
        seccion.setVacantesDisponibles(currentVacancies);

        when(seccionRepository.findById(idSeccion)).thenReturn(Optional.of(seccion));

        // Act & Assert
        assertThatThrownBy(() -> seccionService.updateVacantes(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("La nueva cantidad de vacantes debe superar a las disponibles actualmente");

        verify(seccionRepository, never()).save(any());
        verify(matriculaRepository, never()).matricularPostulantes(any(), any());
    }

    @Test
    void updateVacantes_ShouldThrowBadRequest_WhenNewVacanciesAreEqual() {
        // Arrange
        Integer idSeccion = 1;
        Integer currentVacancies = 20;
        Integer requestedVacancies = 20; // Igual que actual

        UpdateVacantesSeccionRequest request = new UpdateVacantesSeccionRequest();
        request.setIdSeccion(idSeccion);
        request.setCantidadVacantes(requestedVacancies);

        Seccion seccion = new Seccion();
        seccion.setId(idSeccion);
        seccion.setVacantesDisponibles(currentVacancies);

        when(seccionRepository.findById(idSeccion)).thenReturn(Optional.of(seccion));

        // Act & Assert
        assertThatThrownBy(() -> seccionService.updateVacantes(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("La nueva cantidad de vacantes debe superar a las disponibles actualmente");

        verify(seccionRepository, never()).save(any());
        verify(matriculaRepository, never()).matricularPostulantes(any(), any());
    }

    @Test
    void updateVacantes_ShouldThrowNotFound_WhenSectionDoesNotExist() {
        // Arrange
        Integer idSeccion = 999;
        UpdateVacantesSeccionRequest request = new UpdateVacantesSeccionRequest();
        request.setIdSeccion(idSeccion);
        request.setCantidadVacantes(20);

        when(seccionRepository.findById(idSeccion)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> seccionService.updateVacantes(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No se encontró la sección con el ID ingresado");

        verify(seccionRepository, never()).save(any());
        verify(matriculaRepository, never()).matricularPostulantes(any(), any());
    }
}
