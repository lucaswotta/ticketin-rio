package dev.lucaswotta.popinrio.ticketing.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Regras de arquitetura que quebram o build. Sem isso, "o domínio não depende de framework" é só
 * uma intenção no README.
 */
@AnalyzeClasses(
        packages = "dev.lucaswotta.popinrio.ticketing",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta..",
                            "org.hibernate..",
                            "com.fasterxml..",
                            "tools.jackson..")
                    .because("o domínio é Java puro e testável sem subir contexto nenhum");

    @ArchTest
    static final ArchRule domainDoesNotKnowOuterLayers =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..adapter..", "..config..");

    @ArchTest
    static final ArchRule applicationDoesNotKnowAdapters =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..adapter..")
                    .because("casos de uso falam com o mundo só através de portas do domínio");
}
