//package cn.beinet.core.feign.interceptor;
//
//import feign.RequestInterceptor;
//import feign.RequestTemplate;
//
//import java.util.Collection;
//import java.util.Map;
//
///**
// * 在FeignClient请求过程中，透传上下文header
// */
//public class FeignRequestInterceptor implements RequestInterceptor {
//
//    @Override
//    public void apply(RequestTemplate requestTemplate) {
////        if (ConfigVar.isJobApp() || ConfigVar.isAdminApp()) {
////            // job因为会处理所有用户的数据，因此不允许发http请求时设置上下文，避免数据串了
////            // 如果需要设置上下文，请在RestController那边设置
////            return;
////        }
//
//        Map<String, Collection<String>> requestHeaders = requestTemplate.headers();
//        Map<String, String> headers = ContextUtils.getHeaders();
//        headers.forEach((headName, headerValue) -> {
//            if (requestHeaders != null && !requestHeaders.isEmpty()) {
//                //多加一个这个判断防止重复覆盖 调用云资源的接口也需要traceId，防止框架二次覆盖
//                Collection<String> headValue = requestHeaders.get(headName);
//                if (headValue == null || headValue.isEmpty()) {
//                    //说明没有存在重复key
//                    requestTemplate.header(headName, headerValue);
//                }
//            } else {
//                requestTemplate.header(headName, headerValue);
//            }
//        });
//        // 移除BFF标识
//        requestTemplate.removeHeader(ContextConstants.HEADER_BFF);
//    }
//}
