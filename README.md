# Liminal Space — Minecraft Java 1.21.4 (Fabric)

## Que incluye
- Dimension liminal plana de hierba.
- Pequenas montanitas dispersas.
- Pilar gigante de roca con cima de tierra/hierba.
- Escalera exterior hasta la cima.
- Casa explorable: sala, cocina, dormitorio, pasillo y atico.
- La entidad 2D **La Cosa / The Whisperer** dentro de la casa.
- Murmullos, chirridos y movimientos involuntarios.
- Al acercarte pregunta: **"...¿estas solo?"**
- Responder `no`/`n`: te devuelve al mundo normal.
- Responder `si`/`sí`/`s`/`yes`: activa la persecucion.
- Tras responder si, la criatura acelera sus tics: cabeza, mandibula y manos se mueven mucho mas violentamente.
- La criatura es invulnerable.
- Compatible en concepto con shaders y Distant Horizons porque la dimension usa terreno/estructuras vanilla y no requiere un renderizador especial de distancia.

## Requisitos
- Minecraft Java 1.21.4
- Fabric Loader 0.16.10 o superior
- Fabric API 0.119.3+1.21.4 o superior
- Java 21

## Como obtener el .jar
1. Instala Java 21.
2. Abre esta carpeta en Windows.
3. Haz doble clic en `build_mod.bat`.
4. El script descargara Gradle automaticamente y compilara el proyecto.
5. El archivo instalable aparecera en `build/libs/liminal-space-1.0.0.jar`.
6. Copialo a `.minecraft/mods`.
7. Inicia Minecraft 1.21.4 con Fabric.
8. En el mundo escribe `/liminal`.

La primera compilacion necesita internet para descargar Gradle y las dependencias de Fabric/Minecraft.
