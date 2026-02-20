-- DROP SCHEMA public CASCADE;
-- CREATE SCHEMA public;

-- Habilitar la extensión para generar UUIDs automáticamente
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

------------------------------------------------------------------------------
-- CREACIÓN DE TABLAS
------------------------------------------------------------------------------

CREATE TABLE usuarios (
    -- Usamos uuid_generate_v4() para que el ID se cree solo si no lo envías
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombres VARCHAR(30) NOT NULL,
    apellidos VARCHAR(30) NOT NULL,
    correo VARCHAR(35) UNIQUE NOT NULL,
    password VARCHAR(255),
    habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(100),
    -- Usamos TIMESTAMPTZ (con zona horaria) y default actual
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ 
);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(80) NOT NULL
);

CREATE TABLE roles_usuarios (
    id_usuario UUID NOT NULL,
    id_rol INT NOT NULL,
    
    CONSTRAINT fk_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id)
        ON DELETE CASCADE,
    
    CONSTRAINT fk_rol
        FOREIGN KEY (id_rol) REFERENCES roles(id)
        ON DELETE CASCADE,
    
    PRIMARY KEY (id_usuario, id_rol)
);

CREATE TABLE partners (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_client VARCHAR(256) NOT NULL,
    name_client VARCHAR(256) NOT NULL,
    secret_client VARCHAR(256),
    scopes VARCHAR(256),
    grant_types VARCHAR(256),
    authentication_methods VARCHAR(256),
    redirect_uri VARCHAR(256),
    redirect_uri_logout VARCHAR(256)
);

-- Tablas core del proyecto

CREATE TABLE empleados (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_usuario UUID NOT NULL UNIQUE,
    codigo_empleado VARCHAR(20) UNIQUE NOT NULL, -- Identificador institucional
    fecha_ingreso DATE NOT NULL DEFAULT CURRENT_DATE,
    
    -- Campos de Auditoría de Registro
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID, -- ¿Qué administrador dio de alta a este empleado?
    
    CONSTRAINT fk_empleado_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id),
    CONSTRAINT fk_empleado_creator FOREIGN KEY (created_by) REFERENCES empleados(id)
);

create table convocatorias (
	id SERIAL primary key,
	mes VARCHAR(10) not null,
	fecha_inicio date not null,
	fecha_fin date not null,
	estado varchar(10) not null,
	cantidad_vacantes INT NOT null,
	
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by UUID not null,
    
    CONSTRAINT fk_convocatoria_creator FOREIGN KEY (created_by) REFERENCES empleados(id)
);

create table cursos (	
	id SERIAL primary key,
	nombre VARCHAR(90) not null,
	codigo VARCHAR(5) not null,
	modalidad VARCHAR(10) not null,
	
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

create table secciones (
	id SERIAL primary key,
	fecha_inicio date not null,
	id_curso INT not null,
	
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    
    CONSTRAINT fk_seccion_curso FOREIGN KEY (id_curso) REFERENCES cursos(id)
);

create table horarios_seccion (
	id SERIAL primary key,
	id_seccion INT not null,
	dia_semana VARCHAR(9) not null,
	hora_inicio time not null,
	hora_fin time not null,

	CONSTRAINT fk_horario_seccion FOREIGN KEY (id_seccion) REFERENCES secciones(id)
);

create table carreras (
	id SERIAL primary key,
	nombre VARCHAR(45) not null,
	codigo_facultad VARCHAR(6) not null
);

create table estudiantes (
	id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_usuario UUID NOT NULL UNIQUE,
	codigo_estudiante VARCHAR(9) UNIQUE NOT NULL, -- Identificador institucional
    id_carrera INT not NULL,
    celular VARCHAR(9),
    direccion_domicilio VARCHAR(100),
    
    -- Campos de Auditoría de Registro
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    
    constraint fk_estudiante_carrera foreign key (id_carrera) REFERENCES carreras(id),
    CONSTRAINT fk_estudiante_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id)
);

create table evaluaciones_socioeconomicas (
	id SERIAL primary key,
	id_estudiante UUID not null,
	fecha_evaluacion DATE,
	nivel_socioeconomico VARCHAR(10),
	fecha_expiracion DATE,
	
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
	created_by UUID not null,
	constraint fk_evaluacion_socio_estudiante foreign key (id_estudiante) REFERENCES estudiantes(id),
	constraint fk_evaluacion_socio_empleado foreign key (created_by) REFERENCES empleados(id)
);

create table promedios_ponderados (
	id SERIAL primary key,
	id_estudiante UUID not null,
	ciclo_relativo INT not null,
	promedio_ponderado NUMERIC(5,3) not null,
	
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    
    constraint fk_promedio_estudiante foreign key (id_estudiante) REFERENCES estudiantes(id)
);

create table postulaciones (
	id SERIAL primary key,
	id_estudiante UUID not null,
	id_convocatoria INT not null,
	fecha_postulacion date not null default current_date,
	promedio_general NUMERIC(5,3),
	aceptado BOOLEAN,
	
	constraint fk_postulacion_convocatoria foreign key (id_convocatoria) REFERENCES convocatorias(id),
	constraint fk_postulacion_estudiante foreign key (id_estudiante) REFERENCES estudiantes(id)
);

create table cursos_postulacion (
	id_curso int not null,
	id_postulacion int not null,
	
	primary key (id_curso, id_postulacion),
	
	constraint fk_curso foreign key (id_curso) REFERENCES cursos(id),
	constraint fk_postulacion foreign key (id_postulacion) REFERENCES postulaciones(id)
);

create table matriculas (
	id serial primary key,
	id_postulacion int not null,
	id_seccion INT not null,
	fecha_solicitud timestamptz not null DEFAULT CURRENT_TIMESTAMP,
	fecha_matricula timestamptz,
	id_empleado uuid,
	estado VARCHAR(10) not null,
	nota numeric(4,2),
	updated_at TIMESTAMPTZ,
	
	constraint fk_matricula_postulacion foreign key (id_postulacion) references postulaciones(id),
	constraint fk_matricula_seccion foreign key (id_seccion) references secciones(id),
	constraint fk_matricula_empleado foreign key (id_empleado) references empleados(id)
);

-- TRIGGERS PARA AUDITORIA

--------------------------------------------------------------------------------
-- 1. FUNCIÓN GENÉRICA PARA ACTUALIZAR EL TIMESTAMP
--------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--------------------------------------------------------------------------------
-- 2. ASIGNACIÓN DE TRIGGERS A LAS TABLAS CON updated_at
--------------------------------------------------------------------------------

-- Auditoría de Usuarios
CREATE TRIGGER trg_usuarios_updated_at
BEFORE UPDATE ON usuarios
FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();

-- Auditoría de Empleados
CREATE TRIGGER trg_empleados_updated_at
BEFORE UPDATE ON empleados
FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();

-- Auditoría de Convocatorias
CREATE TRIGGER trg_convocatorias_updated_at
BEFORE UPDATE ON convocatorias
FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();

-- Auditoría de Cursos
CREATE TRIGGER trg_cursos_updated_at
BEFORE UPDATE ON cursos
FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();

-- Auditoría de Secciones
CREATE TRIGGER trg_secciones_updated_at
BEFORE UPDATE ON secciones
FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();

-- Auditoría de Estudiantes
CREATE TRIGGER trg_estudiantes_updated_at
BEFORE UPDATE ON estudiantes
FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();

-- Auditoría de Evaluaciones Socioeconómicas
CREATE TRIGGER trg_evaluaciones_socio_updated_at
BEFORE UPDATE ON evaluaciones_socioeconomicas
FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();

-- Auditoría de Promedios Ponderados
CREATE TRIGGER trg_promedios_updated_at
BEFORE UPDATE ON promedios_ponderados
FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();

-- Auditoría de Matrículas
CREATE TRIGGER trg_matriculas_updated_at
BEFORE UPDATE ON matriculas
FOR EACH ROW EXECUTE FUNCTION fn_update_timestamp();
