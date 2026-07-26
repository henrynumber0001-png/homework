package com.homework.web.app.controller.bank;

import com.homework.model.enums.SortType;
import com.homework.web.app.config.WebMvcConfig;
import com.homework.web.app.converter.StringToBaseEnumConverterFactory;
import com.homework.web.app.interceptor.LoginInterceptor;
import com.homework.web.app.service.QuestionBankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class QuestionBankControllerTest {

    @Mock
    private QuestionBankService questionBankService;

    @Mock
    private LoginInterceptor loginInterceptor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService();
        WebMvcConfig webMvcConfig = new WebMvcConfig(
                loginInterceptor,
                new StringToBaseEnumConverterFactory()
        );
        webMvcConfig.addFormatters(conversionService);

        mockMvc = standaloneSetup(new QuestionBankController(questionBankService))
                .setConversionService(conversionService)
                .build();
    }

    @Test
    void numericLatestSortTypeBindsToEnum() throws Exception {
        when(questionBankService.getSortType(SortType.LATEST, 12L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/app/question-banks/group-page/sort-type")
                        .param("sortType", "2")
                        .param("currentSubModuleId", "12"))
                .andExpect(status().isOk());

        verify(questionBankService).getSortType(SortType.LATEST, 12L);
    }
}
