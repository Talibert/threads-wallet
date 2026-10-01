package com.example.threadswallet.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.example.threadswallet", importOptions = ImportOption.DoNotIncludeTests.class)
public class CleanArchitectureTest {

    // 1. Pureza do Domínio: Não pode depender de camadas externas (application ou infra)
    @ArchTest
    static final ArchRule domainShouldNotDependOnApplicationOrInfra =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infra..");

    // 2. Pureza do Domínio: Independência de Framework (Spring)
    @ArchTest
    static final ArchRule domainShouldNotDependOnSpringFramework =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    // 3. Pureza do Domínio: Sem anotações de persistência/JPA no Domínio
    @ArchTest
    static final ArchRule domainShouldNotDependOnJakartaPersistence =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    // 4. Isolamento da Aplicação: Application não depende de Infra
    @ArchTest
    static final ArchRule applicationShouldNotDependOnInfra =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infra..");

    // 5. Isolamento da Aplicação: Application não depende do Spring Web
    @ArchTest
    static final ArchRule applicationShouldNotDependOnSpringWeb =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework.web..");

    // 6. Injeção segura por construtor
    @ArchTest
    static final ArchRule noFieldInjection =
            com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    // 7. Matriz de dependências estrita de Clean Architecture
    @ArchTest
    static final ArchRule layeredArchitectureMustBeRespected =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Domain").definedBy("..domain..")
                    .layer("Application").definedBy("..application..")
                    .layer("Infra").definedBy("..infra..")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infra")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Infra")
                    .whereLayer("Infra").mayNotBeAccessedByAnyLayer();

    // 8. Convenções estruturais: Controllers pertencem à Infra
    @ArchTest
    static final ArchRule controllersShouldResideInInfraController =
            classes().that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage("..infra.controller..");

    // 9. Convenções estruturais: UseCases pertencem à camada de Application
    @ArchTest
    static final ArchRule useCasesShouldResideInApplicationUsecase =
            classes().that().haveSimpleNameEndingWith("UseCase")
                    .should().resideInAPackage("..application..usecase..");

    // 10. Convenções DDD: Repositórios declarados no Domínio devem ser interfaces (DIP)
    @ArchTest
    static final ArchRule domainRepositoriesShouldBeInterfaces =
            classes().that().resideInAPackage("..domain..")
                    .and().haveSimpleNameEndingWith("Repository")
                    .should().beInterfaces();
}
