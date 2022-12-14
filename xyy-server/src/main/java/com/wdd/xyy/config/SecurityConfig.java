package com.wdd.xyy.config;

import com.alibaba.fastjson.JSON;
import com.wdd.xyy.config.component.*;
import com.wdd.xyy.pojo.Admin;
import com.wdd.xyy.pojo.RespBean;
import com.wdd.xyy.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletResponse;

/**
 * @author: wdd
 * @date: 2022/10/22 18:11
 */
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private AdminService adminService;


    @Autowired
    private CustomFilter customFilter;

    @Autowired
    private CustomUrlDecisionManager customUrlDecisionManager;

    @Autowired
    private RestfulAccessDeniedHandler restfulAccessDeniedHandler;

    @Autowired
    private RestAuthorizationEntryPoint restAuthorizationEntryPoint;




    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        //        auth.userDetailsService( adminService::getAdminByUserName);
        //        auth.userDetailsService(username -> adminService.getAdminByUserName(username));
        // ???????????????????????????
        // ?????????????????? ?????????UserDetails
        auth.userDetailsService(userDetailsService())
                .passwordEncoder(passwordEncoder());

    }


    /**
     * ????????????
     *
     * @param web
     * @throws Exception
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(
//                "/login",
                "/register",
                "/css/**",
                "/index.html",
                "favicon.ico",
                "/doc.html",
                "/webjars/**",
                "/swagger-resources/**",
                "/v2/api-docs/**",
                "/swagger-ui.html",
                "/captcha");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf() //?????????csrf
                .disable();
        http.formLogin()
                .loginProcessingUrl("/defaultLogin")
                .successHandler((request, response, authentication) -> {
                    response.setContentType("application/json;charset=utf-8");
                    RespBean ok = RespBean.success("????????????");
                    response.getWriter().write(JSON.toJSONString(ok));
                })
                .failureHandler((request, response, e) -> {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("????????????");
                });
        http.headers()
                .cacheControl();


        http.authorizeRequests()
                .antMatchers("/login").anonymous();
        http.logout().logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                                .logoutSuccessHandler((request, response, authentication) -> {
                                })
                // ???????????????HttpSession??????
                .invalidateHttpSession(true);


//        http.sessionManagement()    //??????cookie ?????????session
//                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                .and()
//                .headers()
//                .cacheControl();
        // ??????????????????
        http.authorizeRequests()
                .anyRequest().authenticated()
                .withObjectPostProcessor(new ObjectPostProcessor<FilterSecurityInterceptor>() {
                    @Override
                    public <O extends FilterSecurityInterceptor> O postProcess(O object) {
                        object.setAccessDecisionManager(customUrlDecisionManager);
                        object.setSecurityMetadataSource(customFilter);
                        return object;
                    }
                });



        // ????????????????????????????????????????????????
        http.exceptionHandling()
                .accessDeniedHandler(restfulAccessDeniedHandler)
                .authenticationEntryPoint(restAuthorizationEntryPoint);
    }


    @Bean
    @Override
    protected UserDetailsService userDetailsService() {
        return (username) -> {
            Admin admin = adminService.getAdminByUserName(username);
            // ??????
            if (admin != null) {
                // ????????????
                admin.setRoles(adminService.getRoles(admin.getId()));
                return admin;
            }
            throw new UsernameNotFoundException("??????????????????");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }



}

