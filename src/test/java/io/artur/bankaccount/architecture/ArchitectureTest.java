package io.artur.bankaccount.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;

class ArchitectureTest {

    private static final String ROOT = "io.artur.bankaccount";

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(new ImportOption.DoNotIncludeTests())
                    .importPackages(ROOT);

    @Test
    void domainMustNotDependOnAnyOuterLayer() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        ROOT + ".application..",
                        ROOT + ".infrastructure..",
                        ROOT + ".api..")
                .because("the domain must stay reusable outside this application")
                .check(CLASSES);
    }

    @Test
    void portsMustNotDependOnSpring() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".application.ports..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .check(CLASSES);
    }

    @Test
    void portPackagesMustContainTopLevelInterfacesOnly() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(ROOT + ".application.ports..")
                .and().areTopLevelClasses()
                .should().beInterfaces()
                .because("a port is a contract, not an implementation")
                .check(CLASSES);
    }

    @Test
    void applicationMustReachTheOutsideThroughPortsOnly() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".infrastructure..",
                        ROOT + ".api..")
                .check(CLASSES);
    }

    @Test
    void presentationMustNotDependOnInfrastructure() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".api..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".infrastructure..")
                .check(CLASSES);
    }

    @Test
    void presentationMustNotUseTheDomainDirectly() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".api..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".domain..")
                .because("aggregates belong behind the use case, not in a controller")
                .check(CLASSES);
    }

    @Test
    void presentationMustUseApplicationContractsOnly() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(ROOT + ".api..")
                .should().onlyDependOnClassesThat(
                        resideOutsideOfPackage(ROOT + ".application..")
                                .or(resideInAnyPackage(
                                        ROOT + ".application.ports.incoming..",
                                        ROOT + ".application.commands.models..",
                                        ROOT + ".application.queries.models..",
                                        ROOT + ".application.queries.readmodels..")))
                .because("presentation uses input ports and data contracts, not application implementations or output ports")
                .check(CLASSES);
    }

    @Test
    void inputContractsMustNotExposeDomainOrAdapters() {
        ArchRuleDefinition.noClasses()
                .that().resideInAnyPackage(
                        ROOT + ".application.ports.incoming..",
                        ROOT + ".application.commands.models..",
                        ROOT + ".application.queries.models..",
                        ROOT + ".application.queries.readmodels..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".domain..", ROOT + ".infrastructure..", ROOT + ".api..",
                        ROOT + ".application.ports.outgoing..",
                        ROOT + ".application.services..", ROOT + ".application.queries.handlers..")
                .check(CLASSES);
    }
}
