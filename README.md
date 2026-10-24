# Autos Colombia - Sistema de gestión de parqueadero

Aplicación web para gestionar la entrada y salida de vehículos del parqueadero "Autos Colombia", que presta el servicio por mensualidad. Corresponde a la **Iteración 1** de la Evidencia de aprendizaje 1 (Diseño de Sistema de información).

- **Institución:** Institución Universitaria Digital de Antioquia
- **Programa:** Ingeniería de Software
- **Docente:** Esteban Montoya
- **Autoras:** Isabela Blandon Velez y Salome Molina Reyes

## Tecnologías

- Java (se probó con Temurin 25; la versión exigida está en `java.version` del `pom.xml`)
- Spring Boot, Spring Web, Spring Data JPA, Spring Security y Validation
- Thymeleaf (pantallas HTML)
- MySQL 8.0 o superior
- Maven (incluido mediante `mvnw`, no hay que instalarlo)

## Funcionalidades

| Historia | Funcionalidad |
|----------|---------------|
| HU-01 | Inicio de sesión con usuario y contraseña (contraseñas cifradas con BCrypt) y roles |
| HU-02 | Registrar la entrada de un vehículo por su placa |
| HU-03 | Validar que la placa pertenezca a un usuario con mensualidad vigente |
| HU-04 | Evitar ingresos duplicados |
| HU-05 | Asignar celda automáticamente; si no hay celda libre, el vehículo entra y queda una novedad para el administrador |
| HU-06 | Registrar la salida, calcular el tiempo de permanencia y liberar la celda |
| HU-07 | Registrar novedades sobre vehículos que están dentro |
| HU-08 | Consultar los vehículos dentro del parqueadero |
| HU-09 | Consultar el historial de entradas y salidas (solo administrador) |
| HU-10 | Consultar novedades (solo administrador) |
| HU-11 | Ver la disponibilidad de celdas |

Formato de placa: carros con 3 letras y 3 números (`ABC123`) y motos con 3 letras, 2 números y 1 letra (`ABC12D`).

## Requisitos previos

1. JDK instalado (`java -version`)
2. MySQL 8.0 o superior en ejecución
3. Git

## Cómo ejecutarlo

### 1. Clonar el repositorio

```
git clone https://github.com/Isabela-B07/autos-colombia-parqueadero.git
cd autos-colombia-parqueadero
```

### 2. Crear la base de datos

Ejecuta el script SQL disponible en este enlace (por ejemplo, desde MySQL Workbench): https://drive.google.com/file/d/1BX3_kQmAg42a6xDaam7LfuSQ8NBMSrT0/view?usp=sharing

El script crea la base de datos `autos_colombia` con todas sus tablas.

### 3. Configurar la conexión

Por seguridad, la contraseña de MySQL no se sube al repositorio. Crea el archivo `src/main/resources/application-local.properties` con este contenido, cambiando `TU_CLAVE` por tu contraseña:

```
spring.datasource.url=jdbc:mysql://localhost:3306/autos_colombia?useSSL=false&serverTimezone=America/Bogota&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=TU_CLAVE
```

### 4. Ejecutar

En Windows (PowerShell) o Linux/Mac:

```
./mvnw spring-boot:run
```

La primera vez descarga Maven y las dependencias, así que puede tardar unos minutos. Cuando aparezca `Started ParqueaderoApplication`, abre:

```
http://localhost:8080
```

## Datos de prueba

Al arrancar con el perfil `local`, el sistema crea datos de prueba si no existen (solo para desarrollo).

**Usuarios del sistema**

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `Admin123` | Administrador |
| `vigilante1` | `Vigilante123` | Vigilante |

**Vehículos**

| Placa | Tipo | Mensualidad |
|-------|------|-------------|
| `ABC123` | Carro | Vigente |
| `XYZ12A` | Moto | Vigente |
| `DEF456` | Carro | Vencida (se rechaza la entrada) |

**Celdas:** `C01` a `C05` (carros), `M01` a `M03` (motos) y `C06` en mantenimiento.

## Pantallas y permisos

| Pantalla | Ruta | Rol |
|----------|------|-----|
| Login | `/login` | Público |
| Panel principal | `/panel` | Vigilante y administrador |
| Registrar entrada | `/entrada` | Vigilante y administrador |
| Registrar salida | `/salida` | Vigilante y administrador |
| Registrar novedad | `/novedades/registrar` | Vigilante y administrador |
| Vehículos dentro | `/consultas/dentro` | Vigilante y administrador |
| Celdas | `/consultas/celdas` | Vigilante y administrador |
| Historial | `/admin/historial` | Solo administrador |
| Consulta de novedades | `/admin/novedades` | Solo administrador |

## Arquitectura

Aplicación web por capas. El código fluye en una sola dirección: `controlador` → `servicio` → `repositorio` → base de datos.

```
src/main/java/com/autoscolombia/parqueadero
├── modelo        Entidades y enumeraciones (Usuario, Cliente, Empleado, Vehiculo,
│                 Mensualidad, Celda, Movimiento, Novedad, Rol, TipoVehiculo,
│                 EstadoCelda, TipoNovedad)
├── repositorio   Único acceso a la base de datos (Spring Data JPA)
├── servicio      Reglas de negocio (MovimientoService, UsuarioService, CeldaService,
│                 NovedadService, ValidacionPlacaService, AuthService,
│                 DetalleEmpleadoService, DatosIniciales)
├── controlador   Reciben las solicitudes de las pantallas
├── seguridad     Cifrado de contraseñas y configuración de roles
└── excepcion     Reglas de negocio incumplidas (placa inválida, mensualidad vencida,
                  ingreso activo, sin celda disponible)
```

Las pantallas están en `src/main/resources/templates` (Thymeleaf) y los estilos en `src/main/resources/static/css`.

**Autenticación:** el inicio de sesión lo ejecuta Spring Security. Consulta al empleado mediante `DetalleEmpleadoService`, compara la contraseña cifrada con BCrypt y aplica los permisos por rol.

**Integridad de los datos:** la base de datos impide dos ingresos activos de una misma placa y que una celda se asigne a dos vehículos a la vez. Los movimientos y las novedades no se eliminan físicamente.

## Documentación del diseño

El diseño completo (requerimientos, historias de usuario, arquitectura, mockups, modelo de datos, casos de uso, diagrama de clases y diagrama de paquetes) está en el documento de la Evidencia de aprendizaje 1.