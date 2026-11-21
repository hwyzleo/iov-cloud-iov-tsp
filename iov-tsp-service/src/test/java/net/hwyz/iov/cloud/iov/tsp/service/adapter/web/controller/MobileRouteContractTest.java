package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 手机端路由契约测试（IOV-TSP-DSN-CR-018 TC018-08/TC018-09）。
 * <p>
 * 通过扫描全部 REST 控制器断言：五个 mobile 端点全部注册、旧 /api/c 不再注册、
 * 车端/服务间/管理端路由前缀不因本次迁移改变。不启动 Spring 上下文。
 *
 * @author hwyz_leo
 */
class MobileRouteContractTest {

    private static final String BASE_PACKAGE = "net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller";

    private static final String MOBILE_PREFIX = "/api/mobile/activation/v1";

    private static Set<String> routes;

    @BeforeAll
    static void collectRoutes() throws Exception {
        routes = new TreeSet<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        for (BeanDefinition candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
            Class<?> controller = ClassUtils.forName(candidate.getBeanClassName(),
                    MobileRouteContractTest.class.getClassLoader());
            RequestMapping typeMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            Set<String> bases = paths(typeMapping);
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (methodMapping == null) {
                    continue;
                }
                for (String base : bases) {
                    for (String path : paths(methodMapping)) {
                        routes.add(httpMethod(methodMapping) + " " + join(base, path));
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("五个手机端端点全部注册在 /api/mobile/activation/v1")
    void mobileEndpointsAreRegistered() {
        assertThat(routes).contains(
                "POST " + MOBILE_PREFIX + "/session/scan",
                "POST " + MOBILE_PREFIX + "/session/cancel",
                "POST " + MOBILE_PREFIX + "/binding/confirm",
                "GET " + MOBILE_PREFIX + "/binding/operations/{operationId}",
                "GET " + MOBILE_PREFIX + "/binding");
    }

    @Test
    @DisplayName("旧 /api/c 手机端路由不再注册，也不存在任何 /api/c 控制器")
    void legacyCRoutesAreRemoved() {
        assertThat(routes).noneMatch(route -> route.contains(" /api/c/"));
        assertThat(routes).doesNotContain(
                "POST /api/c/activation/v1/session/scan",
                "POST /api/c/activation/v1/session/cancel",
                "POST /api/c/activation/v1/binding/confirm",
                "GET /api/c/activation/v1/binding/operations/{operationId}",
                "GET /api/c/activation/v1/binding");
    }

    @Test
    @DisplayName("车端/服务间/管理端路由前缀不因手机端迁移改变")
    void otherCallerScopesRemainUnchanged() {
        assertThat(routes).anyMatch(route -> route.contains(" /api/vehicle/activation/v1/"));
        assertThat(routes).anyMatch(route -> route.contains(" /api/service/"));
        assertThat(routes).anyMatch(route -> route.contains(" /api/mpt/"));
    }

    private static Set<String> paths(RequestMapping mapping) {
        Set<String> result = new LinkedHashSet<>();
        if (mapping == null) {
            result.add("");
            return result;
        }
        for (String value : mapping.value()) {
            result.add(value);
        }
        for (String value : mapping.path()) {
            result.add(value);
        }
        if (result.isEmpty()) {
            result.add("");
        }
        return result;
    }

    private static String httpMethod(RequestMapping mapping) {
        if (mapping.method().length == 0) {
            return "ANY";
        }
        return mapping.method()[0].name();
    }

    private static String join(String base, String path) {
        String joined = (base == null ? "" : base) + (path == null ? "" : path);
        if (joined.isEmpty()) {
            return "/";
        }
        return joined.replaceAll("/{2,}", "/");
    }
}
