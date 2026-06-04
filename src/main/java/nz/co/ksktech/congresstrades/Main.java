package nz.co.ksktech.congresstrades;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Explicit application entry point.
 *
 * <p>Quarkus normally generates its own main at build time, so this class is not
 * required to run via {@code quarkus:dev} or the packaged jar. It exists so IDEs
 * (e.g. IntelliJ's "Quarkus Application" run configuration) have a concrete
 * {@code @QuarkusMain} class to launch. {@code Quarkus.run(args)} boots the app
 * exactly as the generated main would.</p>
 */
@QuarkusMain
public class Main {

    public static void main(String... args) {
        Quarkus.run(args);
    }
}
