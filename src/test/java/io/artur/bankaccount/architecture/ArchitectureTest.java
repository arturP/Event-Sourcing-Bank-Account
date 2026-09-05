package io.artur.bankaccount.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;

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

    // Ratchet only: remove the entry when the controller stops building domain
    // objects and goes through the use case with DTOs instead.
    private static final Set<String> CURRENT_DOMAIN_CONSUMERS_IN_PRESENTATION = Set.of(
            "io.artur.bankaccount.api.controller.AccountController"
    );

    private static final DescribedPredicate<JavaClass> NOT_A_KNOWN_DOMAIN_CONSUMER =
            DescribedPredicate.describe(
                    "not one of the presentation classes that still build domain objects",
                    clazz -> !CURRENT_DOMAIN_CONSUMERS_IN_PRESENTATION.contains(clazz.getName()));

    @Test
    void newPresentationCodeMustNotUseTheDomainDirectly() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(ROOT + ".api..")
                .and(NOT_A_KNOWN_DOMAIN_CONSUMER)
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".domain..")
                .because("aggregates belong behind the use case, not in a controller")
                .check(CLASSES);
    }
}
