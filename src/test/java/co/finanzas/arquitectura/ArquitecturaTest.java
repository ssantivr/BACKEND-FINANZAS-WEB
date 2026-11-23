package co.finanzas.arquitectura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** RNF-09: the hexagonal dependency rule of DEV-01, checked on every build. */
@AnalyzeClasses(packages = "co.finanzas", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest
    static final ArchRule dominioSinDependencias = noClasses().that().resideInAPackage("..dominio..")
            .should().dependOnClassesThat().resideInAnyPackage("..aplicacion..", "..infraestructura..",
                    "org.springframework..", "org.bson..", "jakarta..");

    @ArchTest
    static final ArchRule aplicacionSinInfraestructura = noClasses().that().resideInAPackage("..aplicacion..")
            .should().dependOnClassesThat().resideInAnyPackage("..infraestructura..", "org.springframework..",
                    "org.bson..");

    @ArchTest
    static final ArchRule webSoloUsaPuertosDeEntrada = noClasses().that().resideInAPackage("..infraestructura.web..")
            .should().dependOnClassesThat().resideInAnyPackage("..aplicacion.servicios..", "..aplicacion.puertos.salida..",
                    "..infraestructura.persistencia..");

    @ArchTest
    static final ArchRule serviciosSinCiclos = slices().matching("co.finanzas.(*)..").should().beFreeOfCycles();
}
