package pe.com.security.scholarship.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.security.scholarship.domain.entity.Seccion;
import pe.com.security.scholarship.dto.request.UpdateVacantesSeccionRequest;
import pe.com.security.scholarship.dto.response.UpdatedVacantesSeccionResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.MatriculaRepository;
import pe.com.security.scholarship.repository.SeccionRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private MatriculaRepository matriculaRepository;

    @InjectMocks
    private SeccionService seccionService;

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
