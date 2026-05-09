package github.io.ddmfuhrmann.outfit;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularStructureTest {

    @Test
    void verifiesModularStructure() {
        ApplicationModules modules = ApplicationModules.of(OutfitApplication.class);
        modules.verify();
    }
}
