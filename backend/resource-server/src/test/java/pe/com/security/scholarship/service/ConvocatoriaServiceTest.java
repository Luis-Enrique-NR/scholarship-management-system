package pe.com.security.scholarship.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.transaction.TransactionSystemException;
import pe.com.security.scholarship.domain.entity.Convocatoria;
import pe.com.security.scholarship.domain.entity.Empleado;
import pe.com.security.scholarship.domain.enums.EstadoConvocatoria;
import pe.com.security.scholarship.domain.enums.Mes;
import pe.com.security.scholarship.dto.projection.RankingProjection;
import pe.com.security.scholarship.dto.projection.TasasConvocatoriaProjection;
import pe.com.security.scholarship.dto.request.RegisterConvocatoriaRequest;
import pe.com.security.scholarship.dto.response.AuditEmpleadoResponse;
import pe.com.security.scholarship.dto.response.ConvocatoriaAbiertaResponse;
import pe.com.security.scholarship.dto.response.DetalleConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.HistorialConvocatoriaResponse;
import pe.com.security.scholarship.dto.response.RegisteredConvocatoriaResponse;
import pe.com.security.scholarship.exception.BadRequestException;
import pe.com.security.scholarship.exception.ConflictException;
import pe.com.security.scholarship.exception.InternalServerErrorException;
import pe.com.security.scholarship.exception.NotFoundException;
import pe.com.security.scholarship.repository.ConvocatoriaRepository;
import pe.com.security.scholarship.repository.EmpleadoRepository;
import pe.com.security.scholarship.util.SecurityUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ConvocatoriaServiceTest {

    @Mock
    private ConvocatoriaRepository convocatoriaRepository;

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private EmpleadoService empleadoService;

    @Mock
    private EvaluacionPostulanteService evaluacionPostulanteService;

    @InjectMocks
    private ConvocatoriaService convocatoriaService;

    @AfterEach
    void tearDown() {
        // No static mocks to close here as we use try-with-resources
    }

    @Test
    void crearConvocatoriaExitosamente() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();
        request.setMes(Mes.ENERO);
        request.setFechaInicio(LocalDate.now());
        request.setFechaFin(LocalDate.now().plusDays(30));
        request.setCantidadVacantes(10);

        Empleado empleado = new Empleado();
        empleado.setIdUsuario(idUsuario);

        AuditEmpleadoResponse auditResponse = AuditEmpleadoResponse.builder()
                .codigo("EMP-001")
                .nombreCompleto("Test User")
                .build();

        Convocatoria convocatoriaGuardada = Convocatoria.builder()
                .id(1)
                .mes(request.getMes())
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .estado(EstadoConvocatoria.PROGRAMADO)
                .cantidadVacantes(request.getCantidadVacantes())
                .createdBy(empleado)
                .build();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(convocatoriaRepository.findByYear(anyInt())).thenReturn(Collections.emptyList());
            when(convocatoriaRepository.save(any(Convocatoria.class))).thenReturn(convocatoriaGuardada);
            when(empleadoService.obtenerAuditoriaActual()).thenReturn(auditResponse);

            // Act
            RegisteredConvocatoriaResponse response = convocatoriaService.registerConvocatoria(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMes()).isEqualTo(request.getMes());
            assertThat(response.getCantidadVacantes()).isEqualTo(request.getCantidadVacantes());
            assertThat(response.getCreatedBy()).isEqualTo(auditResponse);
            
            verify(empleadoRepository, times(1)).findByIdUsuario(idUsuario);
            verify(convocatoriaRepository, times(1)).save(any(Convocatoria.class));
            verify(empleadoService, times(1)).obtenerAuditoriaActual();
        }
    }

    @Test
    void crearConvocatoria_DeberiaLanzarBadRequest_CuandoFechasInvalidas() {
        // Arrange
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();
        request.setFechaInicio(LocalDate.now().plusDays(10));
        request.setFechaFin(LocalDate.now()); // Fecha fin anterior a inicio

        // Act & Assert
        assertThatThrownBy(() -> convocatoriaService.registerConvocatoria(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("La fecha de finalización debe ser posterior a la fecha de inicio");
    }

    @Test
    void crearConvocatoria_DeberiaLanzarNotFound_CuandoEmpleadoNoExiste() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();
        request.setFechaInicio(LocalDate.now());
        request.setFechaFin(LocalDate.now().plusDays(30));

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> convocatoriaService.registerConvocatoria(request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("No se encontró empleado con el user id del payload");
        }
    }

    @Test
    void crearConvocatoria_DeberiaLanzarBadRequest_CuandoMesRepetido() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();
        request.setMes(Mes.ENERO);
        request.setFechaInicio(LocalDate.now());
        request.setFechaFin(LocalDate.now().plusDays(30));

        Empleado empleado = new Empleado();
        empleado.setIdUsuario(idUsuario);

        Convocatoria convocatoriaExistente = Convocatoria.builder()
                .mes(Mes.ENERO)
                .estado(EstadoConvocatoria.PROGRAMADO)
                .build();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(convocatoriaRepository.findByYear(anyInt())).thenReturn(List.of(convocatoriaExistente));

            // Act & Assert
            assertThatThrownBy(() -> convocatoriaService.registerConvocatoria(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Ya existe una convocatoria registrada para el mes de ENERO");
        }
    }

    @Test
    void crearConvocatoria_DeberiaPermitirRegistro_CuandoMesRepetidoPeroRechazado() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();
        request.setMes(Mes.ENERO);
        request.setFechaInicio(LocalDate.now());
        request.setFechaFin(LocalDate.now().plusDays(30));
        request.setCantidadVacantes(10);

        Empleado empleado = new Empleado();
        empleado.setIdUsuario(idUsuario);

        AuditEmpleadoResponse auditResponse = AuditEmpleadoResponse.builder().build();

        Convocatoria convocatoriaRechazada = Convocatoria.builder()
                .mes(Mes.ENERO)
                .estado(EstadoConvocatoria.RECHAZADO)
                .fechaInicio(LocalDate.now().minusDays(60))
                .fechaFin(LocalDate.now().minusDays(30))
                .build();
        
        Convocatoria convocatoriaGuardada = Convocatoria.builder()
                .mes(request.getMes())
                .build();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(convocatoriaRepository.findByYear(anyInt())).thenReturn(List.of(convocatoriaRechazada));
            when(convocatoriaRepository.save(any(Convocatoria.class))).thenReturn(convocatoriaGuardada);
            when(empleadoService.obtenerAuditoriaActual()).thenReturn(auditResponse);

            // Act
            RegisteredConvocatoriaResponse response = convocatoriaService.registerConvocatoria(request);

            // Assert
            assertThat(response).isNotNull();
            verify(convocatoriaRepository, times(1)).save(any(Convocatoria.class));
        }
    }

    @Test
    void crearConvocatoria_DeberiaLanzarBadRequest_CuandoPeriodoCruzado() {
        // Arrange
        UUID idUsuario = UUID.randomUUID();
        RegisterConvocatoriaRequest request = new RegisterConvocatoriaRequest();
        request.setMes(Mes.FEBRERO);
        request.setFechaInicio(LocalDate.now().plusDays(10));
        request.setFechaFin(LocalDate.now().plusDays(20));

        Empleado empleado = new Empleado();
        empleado.setIdUsuario(idUsuario);

        // Convocatoria existente que se cruza: empieza hoy y termina en 30 días
        // El request empieza en 10 días y termina en 20 días (está dentro del periodo existente)
        Convocatoria convocatoriaExistente = Convocatoria.builder()
                .mes(Mes.ENERO)
                .estado(EstadoConvocatoria.PROGRAMADO)
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusDays(30))
                .build();

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(idUsuario);
            
            when(empleadoRepository.findByIdUsuario(idUsuario)).thenReturn(Optional.of(empleado));
            when(convocatoriaRepository.findByYear(anyInt())).thenReturn(List.of(convocatoriaExistente));

            // Act & Assert
            assertThatThrownBy(() -> convocatoriaService.registerConvocatoria(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Las fechas seleccionadas presentan un conflicto con otra convocatoria programada");
        }
    }

    @Test
    void actualizarEstadosConvocatorias_DeberiaEvaluarPostulantes_CuandoHayConvocatoriasCerradas() {
        // Arrange
        when(convocatoriaRepository.aperturarConvocatoriasVigentes(any(LocalDate.class))).thenReturn(1);
        when(convocatoriaRepository.cerrarConvocatoriasExpiradas(any(LocalDate.class))).thenReturn(1);
        
        Convocatoria convocatoriaCerrada = new Convocatoria();
        convocatoriaCerrada.setId(1);
        when(evaluacionPostulanteService.evaluarPostulantes()).thenReturn(5);

        // Act
        convocatoriaService.actualizarEstadosConvocatorias();

        // Assert
        verify(convocatoriaRepository, times(1)).aperturarConvocatoriasVigentes(any(LocalDate.class));
        verify(convocatoriaRepository, times(1)).cerrarConvocatoriasExpiradas(any(LocalDate.class));
        // verify(convocatoriaRepository, times(1)).getUltimaConvocatoriaCerrada(); // Eliminado según requerimiento
        verify(evaluacionPostulanteService, times(1)).evaluarPostulantes();
    }

    @Test
    void actualizarEstadosConvocatorias_NoDeberiaEvaluarPostulantes_CuandoNoHayConvocatoriasCerradas() {
        // Arrange
        when(convocatoriaRepository.aperturarConvocatoriasVigentes(any(LocalDate.class))).thenReturn(1);
        when(convocatoriaRepository.cerrarConvocatoriasExpiradas(any(LocalDate.class))).thenReturn(0);


        // Act
        convocatoriaService.actualizarEstadosConvocatorias();

        // Assert
        verify(convocatoriaRepository, times(1)).aperturarConvocatoriasVigentes(any(LocalDate.class));
        verify(convocatoriaRepository, times(1)).cerrarConvocatoriasExpiradas(any(LocalDate.class));
        // verify(convocatoriaRepository, times(1)).getUltimaConvocatoriaCerrada(); // Eliminado según requerimiento
        verify(evaluacionPostulanteService, never()).evaluarPostulantes();
    }

    @Test
    void recover_DeberiaManejarExcepcionSinPropagarla() {
        // Arrange
        TransactionSystemException exception = new TransactionSystemException("Error de transacción");

        // Act & Assert
        assertThatCode(() -> convocatoriaService.recover(exception))
                .doesNotThrowAnyException();
    }

    @Test
    void getConvocatoriaAbierta_DeberiaRetornarConvocatoria_CuandoExiste() {
        // Arrange
        Convocatoria convocatoria = Convocatoria.builder()
                .id(1)
                .mes(Mes.ENERO)
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusDays(30))
                .estado(EstadoConvocatoria.APERTURADO)
                .cantidadVacantes(10)
                .build();

        when(convocatoriaRepository.findConvocatoriaAperturada()).thenReturn(Optional.of(convocatoria));

        // Act
        ConvocatoriaAbiertaResponse response = convocatoriaService.getConvocatoriaAbierta();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMes()).isEqualTo(Mes.ENERO);
        assertThat(response.getCantidadVacantes()).isEqualTo(10);
    }

    @Test
    void getConvocatoriaAbierta_DeberiaLanzarNotFoundException_CuandoNoExiste() {
        // Arrange
        when(convocatoriaRepository.findConvocatoriaAperturada()).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> convocatoriaService.getConvocatoriaAbierta())
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No se encontró ninguna convocatoria abierta");
    }

    @Test
    void getConvocatoriaAbierta_DeberiaLanzarConflictException_CuandoHayMultiplesResultados() {
        // Arrange
        when(convocatoriaRepository.findConvocatoriaAperturada())
                .thenThrow(new IncorrectResultSizeDataAccessException(1));

        // Act & Assert
        assertThatThrownBy(() -> convocatoriaService.getConvocatoriaAbierta())
                .isInstanceOf(ConflictException.class)
                .hasMessage("Existe más de una convocatoria abierta");
    }

    @Test
    void getHistorialConvocatorias_DeberiaRetornarLista_CuandoExistenDatos() {
        // Arrange
        Integer year = 2023;
        Convocatoria convocatoria = Convocatoria.builder()
                .id(1)
                .mes(Mes.ENERO)
                .fechaInicio(LocalDate.of(year, 1, 1))
                .fechaFin(LocalDate.of(year, 1, 31))
                .estado(EstadoConvocatoria.CERRADO)
                .cantidadVacantes(10)
                .build();

        when(convocatoriaRepository.findByYear(year)).thenReturn(List.of(convocatoria));

        // Act
        List<HistorialConvocatoriaResponse> response = convocatoriaService.getHistorialConvocatorias(year);

        // Assert
        assertThat(response).isNotEmpty();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getMes()).isEqualTo(Mes.ENERO);
    }

    @Test
    void getHistorialConvocatorias_DeberiaRetornarListaVacia_CuandoNoExistenDatos() {
        // Arrange
        Integer year = 2023;
        when(convocatoriaRepository.findByYear(year)).thenReturn(Collections.emptyList());

        // Act
        List<HistorialConvocatoriaResponse> response = convocatoriaService.getHistorialConvocatorias(year);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response).isEmpty();
    }
    
    @Test
    void getHistorialConvocatorias_DeberiaRetornarListaVacia_CuandoRepositorioRetornaNull() {
        // Arrange
        Integer year = 2023;
        when(convocatoriaRepository.findByYear(year)).thenReturn(null);

        // Act
        List<HistorialConvocatoriaResponse> response = convocatoriaService.getHistorialConvocatorias(year);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response).isEmpty();
    }

    @Test
    void getDetalleConvocatoria_DeberiaRetornarDetalleCompleto_CuandoTodoEsValido() {
        // Arrange
        Integer id = 1;
        Convocatoria convocatoria = Convocatoria.builder()
                .id(id)
                .mes(Mes.ENERO)
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusDays(30))
                .estado(EstadoConvocatoria.APERTURADO)
                .cantidadVacantes(100)
                .build();

        AuditEmpleadoResponse auditResponse = AuditEmpleadoResponse.builder()
                .codigo("EMP-001")
                .nombreCompleto("Admin")
                .build();

        TasasConvocatoriaProjection tasas = mock(TasasConvocatoriaProjection.class);
        when(tasas.getTasaAceptacion()).thenReturn(0.75);
        when(tasas.getTasaMatriculados()).thenReturn(60.0);

        RankingProjection ranking1 = mock(RankingProjection.class);

        when(convocatoriaRepository.findById(id)).thenReturn(Optional.of(convocatoria));
        when(empleadoService.obtenerAuditoriaActual()).thenReturn(auditResponse);
        when(convocatoriaRepository.getCantidadPostulantes(id)).thenReturn(100);
        when(convocatoriaRepository.getTasasGenerales(id)).thenReturn(tasas);
        when(convocatoriaRepository.getRankingSocioeconomico(id)).thenReturn(List.of(ranking1));
        when(convocatoriaRepository.getRankingCiclo(id)).thenReturn(List.of(ranking1));
        when(convocatoriaRepository.getRankingCarrera(id)).thenReturn(List.of(ranking1));

        // Act
        DetalleConvocatoriaResponse response = convocatoriaService.getDetalleConvocatoria(id);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getDatosGeneralesConvocatoria().getMes().equals(Mes.ENERO));
        assertThat(response.getCantidadPostulantes()).isEqualTo(100);
        assertThat(response.getTasaAceptacion()).isEqualTo("75%");
        assertThat(response.getRankingSocioeconomico()).hasSize(1);
    }

    @Test
    void getDetalleConvocatoria_DeberiaLanzarNotFoundException_CuandoConvocatoriaNoExiste() {
        // Arrange
        Integer id = 1;
        when(convocatoriaRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> convocatoriaService.getDetalleConvocatoria(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("No se encontró convocatoria con el id especificado");

        verify(convocatoriaRepository, times(0)).getTasasGenerales(anyInt());
    }

    @Test
    void getDetalleConvocatoria_DeberiaLanzarInternalServerError_CuandoFallaCalculoTasas() {
        // Arrange
        Integer id = 1;
        Convocatoria convocatoria = Convocatoria.builder().id(id).build();

        when(convocatoriaRepository.findById(id)).thenReturn(Optional.of(convocatoria));
        when(empleadoService.obtenerAuditoriaActual()).thenReturn(AuditEmpleadoResponse.builder().build());
        when(convocatoriaRepository.getCantidadPostulantes(id)).thenReturn(100);
        when(convocatoriaRepository.getTasasGenerales(id)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> convocatoriaService.getDetalleConvocatoria(id))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessage("No se pudieron calcular las tasas de la convocatoria");
    }

    @Test
    void getDetalleConvocatoria_DeberiaManejarListasVaciasEnRankings() {
        // Arrange
        Integer id = 1;
        Convocatoria convocatoria = Convocatoria.builder()
                .id(id)
                .mes(Mes.ENERO)
                .build();

        TasasConvocatoriaProjection tasas = mock(TasasConvocatoriaProjection.class);
        when(tasas.getTasaAceptacion()).thenReturn(0.0);
        when(tasas.getTasaMatriculados()).thenReturn(0.0);

        when(convocatoriaRepository.findById(id)).thenReturn(Optional.of(convocatoria));
        when(empleadoService.obtenerAuditoriaActual()).thenReturn(AuditEmpleadoResponse.builder().build());
        when(convocatoriaRepository.getCantidadPostulantes(id)).thenReturn(0);
        when(convocatoriaRepository.getTasasGenerales(id)).thenReturn(tasas);
        when(convocatoriaRepository.getRankingSocioeconomico(id)).thenReturn(Collections.emptyList());
        when(convocatoriaRepository.getRankingCiclo(id)).thenReturn(Collections.emptyList());
        when(convocatoriaRepository.getRankingCarrera(id)).thenReturn(Collections.emptyList());

        // Act
        DetalleConvocatoriaResponse response = convocatoriaService.getDetalleConvocatoria(id);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getRankingSocioeconomico()).isEmpty();
        assertThat(response.getRankingCiclos()).isEmpty();
        assertThat(response.getRankingCarreras()).isEmpty();
    }
}
