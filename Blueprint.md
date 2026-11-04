# Blueprint: this codebase seen from .NET

I come from C# and .NET. This page maps every Java, Spring and Maven idea used here to its
nearest .NET counterpart, so the design can be read in either language. Paths point at the
code that uses each idea.

## Architecture

| Here (Java) | .NET counterpart | Notes |
|---|---|---|
| Mono repository of client libraries (`core`, `countries`, `visits`) | Azure SDK for .NET (`Azure.Core`, `Azure.Storage.Blobs`, ...) | One package per service, a `<Service>Client` built from options, a shared core |
| `service` module | An ASP.NET Core web project | The one deployable; would normally live in its own repository |
| [`HttpPipeline`](core/src/main/java/se/arbetsprov/core/http/HttpPipeline.java) | `Azure.Core.Pipeline.HttpPipeline`, or `HttpClient` with a chain of `DelegatingHandler`s | Request down through policies, response back up in reverse |
| [`HttpPipelinePolicy`](core/src/main/java/se/arbetsprov/core/http/policy/HttpPipelinePolicy.java) | `HttpPipelinePolicy` / `DelegatingHandler.SendAsync` | `next.process(request)` is `base.SendAsync(request)` |
| [`RetryPolicy`](core/src/main/java/se/arbetsprov/core/http/policy/RetryPolicy.java) + `RetryStrategy` | Polly retry with exponential backoff, or `Azure.Core.RetryPolicy` | A skeleton here |
| [`AuthorizationPolicy`](core/src/main/java/se/arbetsprov/core/http/policy/AuthorizationPolicy.java) + `KeyCredential` | `BearerTokenAuthenticationPolicy` + `AzureKeyCredential` | Adds `Authorization: Bearer <key>` |
| [`ClientOptions`](core/src/main/java/se/arbetsprov/core/client/ClientOptions.java) | `Azure.Core.ClientOptions` | Subclassed per client (`CountriesClientOptions`, `VisitsClientOptions`) |
| [`CountryDirectory`](visits/src/main/java/se/arbetsprov/visits/CountryDirectory.java) | A one-method interface, or a `Func<string, bool>` | Lets visits check countries without referencing the countries package |
| [`VisitsBackend.IN_PROCESS`](visits/src/main/java/se/arbetsprov/visits/VisitsBackend.java) | An enum selecting an implementation in DI | A remote backend would plug in behind the same client |

## Language

| Java | C# | Where |
|---|---|---|
| `record Country(String code, ...)` | `record Country(string Code, ...)` | [`Country`](countries/src/main/java/se/arbetsprov/countries/Country.java) |
| Compact constructor `public Country { ... }` | Validation in a primary constructor or `init` accessors | Records |
| `final class` | `sealed class` | Most classes |
| `var` | `var` | Everywhere |
| `enum` | `enum` (but Java enums are classes and can have methods) | `CountrySort`, `RetryStrategy` |
| `switch` expression with `->` and `yield` | `switch` expression; `yield` returns a value from a block arm | [`InMemoryCountryCache`](countries/src/main/java/se/arbetsprov/countries/cache/InMemoryCountryCache.java) |
| Text block `"""..."""` | Raw string literal `"""..."""` | Tests, `OpenApiConfiguration` |
| Annotation `@Something` | Attribute `[Something]` | Spring, JUnit |
| Method reference `Country::name` | Method group, or `c => c.Name` | Comparators, mapping |
| `Function.identity()` | `x => x` | `toUnmodifiableMap(..., Function.identity())` |
| Functional interface (`Function<T,R>`, `UnaryOperator<T>`) | `Func<T,TResult>` delegates | `VisitCache.update` |
| `Optional<T>` | Nullable reference `T?` | `CountriesClient.get` |
| `RuntimeException` subclasses | Exceptions (C# has no checked exceptions) | `VisitNotFoundException` |
| `UncheckedIOException` wrapping `IOException` | `HttpRequestException` / `IOException` | [`JdkHttpClient`](core/src/main/java/se/arbetsprov/core/http/JdkHttpClient.java) |
| `toString()` override hiding the key | `ToString()` override | [`KeyCredential`](core/src/main/java/se/arbetsprov/core/credential/KeyCredential.java) |

## Collections and queries

| Java | C# / LINQ |
|---|---|
| `list.stream()` | `list.AsEnumerable()` |
| `.filter(c -> ...)` | `.Where(c => ...)` |
| `.map(...)` | `.Select(...)` |
| `.sorted(comparator)` | `.OrderBy(...)` |
| `Comparator.comparing(Country::name, collator).thenComparing(Country::code)` | `.OrderBy(c => c.Name, comparer).ThenBy(c => c.Code)` |
| `comparator.reversed()` | `OrderByDescending` |
| `.toList()` | `.ToList()` (Java's result is unmodifiable) |
| `Collectors.toUnmodifiableMap(k, v)` | `.ToDictionary(k, v)` / `.ToFrozenDictionary(...)` |
| `List.copyOf(...)`, `List.of(...)` | `ImmutableList` / `[..]` collection expressions |
| `subList(from, to)` | `.Skip(from).Take(to - from)` |
| `Collator` (English, primary strength) | `StringComparer.Create(CultureInfo.GetCultureInfo("en"), CompareOptions.IgnoreCase \| IgnoreNonSpace)` |

## Concurrency

| Java | C# | Where |
|---|---|---|
| Virtual threads (`spring.threads.virtual.enabled`) | `async`/`await` on the thread pool, without having to mark methods `async` | The whole service |
| Blocking `client.send(...)` on a virtual thread | `await httpClient.SendAsync(...)` | `JdkHttpClient` |
| `Executors.newVirtualThreadPerTaskExecutor()` | `Task.Run` per item / `Parallel.ForEachAsync` | Tests |
| `ConcurrentHashMap` | `ConcurrentDictionary` | [`InMemoryVisitCache`](visits/src/main/java/se/arbetsprov/visits/cache/InMemoryVisitCache.java) |
| `putIfAbsent` | `TryAdd` | Create visit |
| `computeIfPresent` | `TryUpdate` loop / `AddOrUpdate` | Update note |
| `remove(key)` | `TryRemove` | Delete visit |
| `computeIfAbsent` | `GetOrAdd` | Per-user map |
| `volatile` field swapped as a whole | `volatile` / `Volatile.Write`, or `Interlocked.Exchange` | `InMemoryCountryCache.contents` |
| `AtomicInteger` | `Interlocked.Increment` | Tests |
| `CountDownLatch` | `CountdownEvent` / `TaskCompletionSource` | Tests |
| `java.time.Clock` | `TimeProvider` (.NET 8) | `VisitsClientOptions.clock` |

## HTTP and JSON

| Java | C# |
|---|---|
| `java.net.http.HttpClient` | `System.Net.Http.HttpClient` |
| `URI` | `Uri` |
| Jackson `JsonMapper` | `System.Text.Json.JsonSerializer` |
| `@JsonProperty("alpha_3")` | `[JsonPropertyName("alpha_3")]` |
| `FAIL_ON_UNKNOWN_PROPERTIES` disabled | The default in System.Text.Json |
| Reading `List<T>` via `constructCollectionType` | `JsonSerializer.Deserialize<List<T>>(...)` |

## Spring Boot and ASP.NET Core

| Spring | ASP.NET Core | Where |
|---|---|---|
| `@SpringBootApplication` + `SpringApplication.run` | `WebApplication.CreateBuilder(args)` ... `app.Run()` | [`ApiApplication`](service/src/main/java/se/arbetsprov/service/ApiApplication.java) |
| `@Configuration` + `@Bean` | `builder.Services.AddSingleton(...)` | [`ClientsConfiguration`](service/src/main/java/se/arbetsprov/service/config/ClientsConfiguration.java) |
| Constructor injection | Constructor injection | Controllers |
| `@ConfigurationProperties("countries") record` | `services.Configure<CountriesOptions>(config.GetSection("countries"))` + `IOptions<T>` | `CountriesProperties` |
| `application.yml` | `appsettings.json` | [`application.yml`](service/src/main/resources/application.yml) |
| `${RESTCOUNTRIES_API_KEY:}` | Environment variables configuration provider | `countries.api-key` |
| `ApplicationRunner` | `IHostedService.StartAsync` | [`CountriesLoader`](service/src/main/java/se/arbetsprov/service/config/CountriesLoader.java) |
| Readiness after runners finish | A health check that reports healthy once loading is done | Actuator readiness |
| `@RestController` + `@RequestMapping` | `[ApiController]` + `[Route]` | Controllers |
| `@GetMapping`, `@PostMapping`, ... | `[HttpGet]`, `[HttpPost]`, ... | Controllers |
| `@RequestParam`, `@PathVariable`, `@RequestHeader`, `@RequestBody` | `[FromQuery]`, `[FromRoute]`, `[FromHeader]`, `[FromBody]` | Controllers |
| `ResponseEntity.created(uri).body(...)` | `Created(uri, body)` / `Results.Created(...)` | `VisitsController.create` |
| `ResponseStatusException(NOT_FOUND, ...)` | `return NotFound(...)` / `Results.Problem(...)` | Controllers |
| `ProblemDetail` + `@RestControllerAdvice` | `ProblemDetails` + `IExceptionHandler` (.NET 8) | [`ApiExceptionHandler`](service/src/main/java/se/arbetsprov/service/error/ApiExceptionHandler.java) |
| `WebMvcConfigurer.addFormatters` (case-insensitive enums) | A custom model binder or type converter | [`WebConfiguration`](service/src/main/java/se/arbetsprov/service/config/WebConfiguration.java) |
| Actuator `/actuator/health/liveness`, `/readiness` | `AddHealthChecks()` + `MapHealthChecks(...)` | `application.yml` |
| springdoc-openapi + Swagger UI | Swashbuckle / `Microsoft.AspNetCore.OpenApi` | `OpenApiConfiguration` |
| `@Parameter`, `@Operation`, `@ApiResponse` | XML comments, `[ProducesResponseType]`, `WithOpenApi` | Controllers |
| Meta-annotation [`@UserId`](service/src/main/java/se/arbetsprov/service/visits/UserId.java) | A custom attribute / operation filter | `X-User-Id` header |
| `redirect:/swagger-ui.html` | `Results.Redirect(...)` | `RootController` |

## Build

| Maven | .NET | Where |
|---|---|---|
| `pom.xml` per module | `.csproj` per project | Modules |
| Root parent POM with `<modules>` | `.sln` + `Directory.Build.props` | [`pom.xml`](pom.xml) |
| `dependencyManagement` importing the Spring Boot BOM | `Directory.Packages.props` (central package management) | Root POM |
| `mvnw` (Maven wrapper) | `global.json` pinning the SDK | Repository root |
| `./mvnw verify` | `dotnet build` + `dotnet test` | `eng/scripts/build` |
| `spring-boot-maven-plugin` repackage to one jar | `dotnet publish` | `service/pom.xml` |
| `<parameters>true</parameters>` | No equivalent needed; parameter names are always in metadata | Root POM |
| Gradle convention plugins | Shared MSBuild `.targets` / SDK packages | Not used; see the README |

## Tests

| Java | .NET | Where |
|---|---|---|
| JUnit `@Test` | xUnit `[Fact]` | All tests |
| `@DisplayName` | `[Fact(DisplayName = ...)]` | Test classes |
| `@BeforeEach` / `@AfterEach` | Constructor / `IDisposable.Dispose` | WireMock tests |
| `@Disabled` | `[Fact(Skip = ...)]` | Test shells before review |
| AssertJ `assertThat(...)` | FluentAssertions / Shouldly | All tests |
| WireMock | WireMock.Net | Import tests |
| `@SpringBootTest(webEnvironment = RANDOM_PORT)` | `WebApplicationFactory<Program>` | [`service` tests](service/src/test/java/se/arbetsprov/service) |
| `@DynamicPropertySource` | `WithWebHostBuilder(b => b.ConfigureAppConfiguration(...))` | `LiveImportTests` |
| `@TestConfiguration` + `@Import` | `ConfigureTestServices` | `StartupTests.ThreadProbe` |

## Container and CI

| Here | .NET |
|---|---|
| `eclipse-temurin:25-jdk` build stage | `mcr.microsoft.com/dotnet/sdk` |
| `eclipse-temurin:25-jre` runtime stage | `mcr.microsoft.com/dotnet/aspnet` |
| `java -jar service.jar` | `dotnet Service.dll` |
| `actions/setup-java` | `actions/setup-dotnet` |
