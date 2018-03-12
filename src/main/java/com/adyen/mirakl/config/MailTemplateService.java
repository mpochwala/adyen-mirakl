package com.adyen.mirakl.config;

import com.adyen.mirakl.service.MailService;
import com.mirakl.client.mmp.domain.shop.MiraklShop;
import io.github.jhipster.config.JHipsterProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import java.util.Locale;

@Service
public class MailTemplateService {

    private static final String MIRAKL_SHOP = "miraklShop";
    private static final String MIRAKL_CALL_BACK_SHOP_URL = "miraklCallBackShopUrl";
    private static final String BASE_URL = "baseUrl";

    @Value("${miraklOperator.miraklEnvUrl}")
    private String miraklEnvUrl;

    private final JHipsterProperties jHipsterProperties;
    private final MailService mailService;
    private final SpringTemplateEngine templateEngine;
    private final MessageSource messageSource;

    public MailTemplateService(final JHipsterProperties jHipsterProperties, MailService mailService, SpringTemplateEngine templateEngine, MessageSource messageSource) {
        this.jHipsterProperties = jHipsterProperties;
        this.mailService = mailService;
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
    }

    @Async
    public void sendMiraklShopEmailFromTemplate(MiraklShop miraklShop, Locale locale, String templateName, String titleKey) {
        Context context = new Context(locale);
        context.setVariable(MIRAKL_SHOP, miraklShop);
        context.setVariable(MIRAKL_CALL_BACK_SHOP_URL, String.format("%s/mmp/shop/account/shop/%s", miraklEnvUrl, miraklShop.getId()));
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);
        mailService.sendEmail(miraklShop.getContactInformation().getEmail(), subject, content, false, true);
    }



    public String getMiraklEnvUrl() {
        return miraklEnvUrl;
    }

    public void setMiraklEnvUrl(final String miraklEnvUrl) {
        this.miraklEnvUrl = miraklEnvUrl;
    }
}