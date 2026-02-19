# **DESCRIPCIÓN DEL BACKEND**

## 🔐 Distributed Security Infrastructure (OAuth2 / OIDC)

Este componente del backend establece una arquitectura de seguridad moderna y desacoplada para el **Scholarship Management System**, utilizando los estándares **OAuth2** y **OpenID Connect (OIDC)**.

La solución se divide en dos microservicios independientes que garantizan la integridad de los datos y la escalabilidad del sistema.

---

### 🏛️ Arquitectura del Sistema



#### **1. Authorization Server (Puerto 9000)**
**Tecnología:** Spring Boot 3 + Spring Authorization Server.

Actúa como la **Autoridad Central de Identidad**, centralizando la autenticación y eliminando la carga de seguridad de la lógica de negocio.

* **Firma Criptográfica:** Implementación de llaves **RSA (RSAKey)** para la firma digital de tokens JWT, garantizando la autenticidad y el no repudio.
* **Token Customization:** Uso de `OAuth2TokenCustomizer` para inyectar *claims* personalizados (`uid`, `roles`, `name`). Esto permite que el Resource Server obtenga contexto del usuario sin realizar consultas adicionales a la base de datos.
* **Protocolo Estándar:** Exposición de endpoints bajo el estándar OIDC (`/.well-known/openid-configuration`), permitiendo la integración futura con cualquier cliente (Web, Móvil o terceros).

---

#### **2. Resource Server (Puerto 8080)**
**Tecnología:** Spring Boot 3 + Spring OAuth2 Resource Server.

Es el **Core del Negocio**. Aquí reside la lógica de gestión de becas, alumnos y convocatorias, protegida bajo una capa de validación estricta.

* **Validación Stateless:** El servidor no mantiene sesiones (sin estado). Valida los tokens en tiempo real mediante la clave pública del Auth Server (`JwtDecoders.fromIssuerLocation`), lo que permite escalabilidad horizontal.
* **Control de Acceso (RBAC):** Implementación de un `JwtAuthenticationConverter` personalizado para mapear los *claims* del token a autoridades de Spring Security.
* **Seguridad Granular:** Uso de anotaciones `@PreAuthorize` para proteger endpoints críticos (ej. gestión de convocatorias exclusiva para administradores).

> "API REST segura y escalable que delega la confianza en firmas criptográficas, asegurando que cada transacción sea auditada y autorizada."

---

#### ⚙️ Flujo de Comunicación (Under the Hood)

1.  **Autenticación:** El usuario solicita acceso al **Authorization Server**.
2.  **Emisión:** Tras validar credenciales, el servidor firma un **JWT** con su **Clave Privada**.
3.  **Consumo:** El cliente envía el JWT en el header `Authorization: Bearer` al **Resource Server**.
4.  **Verificación:** El Resource Server utiliza la **Clave Pública** para validar matemáticamente que el token es legítimo y no ha sido alterado.

---

#### 🚀 Impacto en el Proyecto
* **Desacoplamiento Total:** La lógica de usuarios y la de becas viven en contextos distintos.
* **Seguridad de Grado Empresarial:** Prevención de ataques comunes mediante el uso de UUIDs y tokens firmados.
* **Preparado para el Futuro:** Arquitectura lista para migrar a microservicios o integrar login con redes sociales (Google, LinkedIn) de forma transparente.