package org.openpaas.ieda.deploy.web.config.publicStemcell;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.FileInputStream;
import java.security.Principal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openpaas.ieda.OpenpaasIedaControllerApplication;
import org.openpaas.ieda.TestBeansConfiguration;
import org.openpaas.ieda.common.api.LocalDirectoryConfiguration;
import org.openpaas.ieda.deploy.web.common.BaseControllerTest;
import org.openpaas.ieda.deploy.web.common.service.CommonDeployUtils;
import org.openpaas.ieda.deploy.web.config.stemcell.dto.StemcellManagementDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {OpenpaasIedaControllerApplication.class, TestBeansConfiguration.class})
@WebAppConfiguration
@Transactional
@TransactionConfiguration(defaultRollback=true)
public class StemcellManagementControllerTest extends BaseControllerTest{
    
    @Autowired WebApplicationContext wac;
    @Autowired StemcellManagementServiceTest service;
    
    private MockMvc mockMvc;
    protected MockRestServiceServer mockServer;
    private Principal principal = null;
    
    private final static Logger LOGGER = LoggerFactory.getLogger(StemcellManagementControllerTest.class);
    final private static String LOCK_DIR=LocalDirectoryConfiguration.getLockDir();
    final private static String SEPARATOR = System.getProperty("file.separator");
    final private static String dUMMY_PATH = System.getProperty("user.dir") + SEPARATOR + "test/main/java/org/openpaas/ieda/deploy/web/assets/dummy-stemcell.tgz";
    final private static String STEMCELL_PATH = LocalDirectoryConfiguration.getStemcellDir();
    final private static String LOCK_FILE= "light-bosh-stemcell-2820-aws-xen-hvm-ubuntu-trusty-go_agent-download.lock";
    
    /*************************************** URL *******************************************/
    final static String VIEW_URL = "/config/stemcell"; // 스템셀 관리 화면 요청
    final static String PUBLIC_STEMCELL_LIST_URL = "/config/stemcell/list";
    final static String PUBLIC_STEMCELL_REGIST_URL = "/config/stemcell/regist/info/Y";
    final static String PUBLIC_STEMCELL_FILE_UPLOAD_URL = "/config/stemcell/regist/upload";
    final static String PUBLIC_STEMCELL_DELETE_URL = "/config/stemcell/delete";
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : 하나의 메소드가 실행되기전 호출
     * @title : setUp
     * @return : void
    *****************************************************************/
    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        
        principal = getLoggined();
    }
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : 스템셀 관리 화면 요청
     * @title : testGoStemcellManagement
     * @return : void
    *****************************************************************/
    @Test
    public void testGoStemcellManagement() throws Exception {
        
        if(LOGGER.isInfoEnabled()){  LOGGER.info("================================== testGoStemcellManagement"); }
        ResultActions result = mockMvc.perform(get(VIEW_URL)
                .contentType(MediaType.APPLICATION_JSON));
        
        result.andDo(MockMvcResultHandlers.print())
        .andReturn();
    }
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : 스템셀 목록 정보 조회
     * @title : testGetPublicStemcells
     * @return : void
    *****************************************************************/
    @Test
    public void testGetPublicStemcells() throws Exception {
        if(LOGGER.isInfoEnabled()){  LOGGER.info("================================== testGetPublicStemcells"); }
        
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get(PUBLIC_STEMCELL_LIST_URL)
                .contentType(MediaType.APPLICATION_JSON));
        
        result.andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        
    }
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : URL을 통한 스템셀 등록 
     * @title : testSavePublicStemcellUrl
     * @return : void
    *****************************************************************/
    @Test
    public void testSavePublicStemcellUrl() throws Exception{
        if(LOGGER.isInfoEnabled()){  LOGGER.info("=================  시스템 스템셀 등록 TEST START  ================="); }
        
        for(int i=0; i<2; i++ ){
            switch(i){
            // 시스템 스템셀 입력 정보 저장 (url)
            case 0 : testRegistStemcellInfo("url");
                     break;
            case 1 : testPublicStemcellDownloading("url");
                     break;
            default : break;
            }
            CommonDeployUtils.deleteFile(LOCK_DIR, LOCK_FILE);
        }
    }
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : Version을 통한 스템셀 등록 
     * @title : testSavePublicStemcellVersion
     * @return : void
    *****************************************************************/
    @Test
    public void testSavePublicStemcellVersion() throws Exception{
        if(LOGGER.isInfoEnabled()){  LOGGER.info("=================  testSavePublicStemcellVersion"); }
        
        for(int i=0; i<2; i++ ){
            switch(i){
            // 시스템 스템셀 입력 정보 저장 (url)
            case 0 : testRegistStemcellInfo("version");
                     break;
            case 1 : testPublicStemcellDownloading("version");
                     break;
            default : break;
            }
            CommonDeployUtils.deleteFile(LOCK_DIR, LOCK_FILE);
        }
    }
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : 로컬 File을 통한 스템셀 다운로드
     * @title : testDoPublicStemcellUpload
     * @return : void
    *****************************************************************/
    @Test
    public void testDoPublicStemcellUpload() throws Exception {
        if(LOGGER.isInfoEnabled()){  LOGGER.info("=================  로컬 File을 통한 스템셀 다운로드 TEST START  ================="); }
        FileInputStream inputFile = new FileInputStream(dUMMY_PATH);
        MockMultipartFile firstFile = new MockMultipartFile("file", "light-bosh-stemcell-2820-aws-xen-hvm-ubuntu-trusty-go_agent.tgz", "multipart/form-data", inputFile);
        
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.fileUpload(PUBLIC_STEMCELL_FILE_UPLOAD_URL)
                                .file(firstFile)
                                .param("overlay", "true")
                                .param("id", "1"));
                
        result.andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
    }
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : 스템셀 삭제
     * @title : testDeleteStemcell
     * @return : void
    *****************************************************************/
    @Test
    public void testDeleteStemcell() throws Exception{
        if(LOGGER.isInfoEnabled()){  LOGGER.info("=================  스템셀 삭제 TEST START  ================="); }
        testSavePublicStemcellUrl();
        String requestJson = setStemcellDeleteData();
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.delete(PUBLIC_STEMCELL_DELETE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson));
        
        result.andDo(MockMvcResultHandlers.print())
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
    }
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : URL, Version을 통한 스템셀 다운로드
     * @title : testPublicStemcellDownloading
     * @return : void
    *****************************************************************/
    private void testPublicStemcellDownloading(String fileType) {
        if(LOGGER.isInfoEnabled()){  LOGGER.info("=================  URL을 통한 스템셀 다운로드 TEST START  ================="); }
        StemcellManagementDTO.Regist dto = setStemcellDownload(fileType);
        service.teststemcellDownloadAsync(dto, principal);
    }

    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : 스템셀정보 저장
     * @title : testRegistStemcellInfo
     * @return : void
    *****************************************************************/
    public void testRegistStemcellInfo(String fileType) throws Exception{
        if(LOGGER.isInfoEnabled()){  LOGGER.info("=================  testRegistStemcellInfo"); }
        
        String requestJson = "";
        if("url".equals(fileType)){
            requestJson = setStemcellRegistInfoUrl(fileType);
        }else if("version".equals(fileType)){
            requestJson = setStemcellRegistInfoVersion(fileType);
        }else if("file".equals(fileType)){
            requestJson = setStemcellRegistInfoFile(fileType);
        }
        
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.post(PUBLIC_STEMCELL_REGIST_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .param("testFlag", "Y")
                .principal(principal)
                .content(requestJson));
            
        result.andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
    }
    
    
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : 하나의 메소드가 동작한 직후 실행
     * @title : tearDown
     * @return : void
    *****************************************************************/
    @After
    public void tearDown(){
        //delete deployment stemcell File
        CommonDeployUtils.deleteFile(STEMCELL_PATH, "light-bosh-stemcell-2820-aws-xen-hvm-ubuntu-trusty-go_agent.tgz");
        
        CommonDeployUtils.deleteFile(LOCK_DIR, LOCK_FILE);
    }
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : URL을 통한 스템셀 정보 셀설정 
     * @title : setStemcellRegistInfoUrl
     * @return : String
    *****************************************************************/
    public String setStemcellRegistInfoUrl(String fileType) throws Exception{
        StemcellManagementDTO.Regist dto = new StemcellManagementDTO.Regist();
        dto.setId(1);
        dto.setStemcellUrl("https://bosh-jenkins-artifacts.s3.amazonaws.com/bosh-stemcell/aws/light-bosh-stemcell-2820-aws-xen-hvm-ubuntu-trusty-go_agent.tgz");
        dto.setStemcellName("testStemcellFile");
        dto.setStemcellFileName("testFileName");
        dto.setStemcellVersion("1111");
        dto.setStemcellSize("123456789");
        dto.setOsName("UBUNTU");
        dto.setIaasType("OPENSTACK");
        dto.setOsVersion("TRUSTY");
        dto.setLight("true");
        dto.setFileType(fileType);
        dto.setOverlayCheck("true");
        dto.setCreateUserId("tester");
        dto.setUpdateUserId("tester");
        
        //JSON 형태로 변환
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson= ow.writeValueAsString(dto);
        
        return requestJson;
    }
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : 스템셀 다운로드 설정
     * @title : setStemcellDownload
     * @return : StemcellManagementDTO.Regist
    *****************************************************************/
    public StemcellManagementDTO.Regist setStemcellDownload(String fileType) {
        StemcellManagementDTO.Regist dto = new StemcellManagementDTO.Regist();
        dto.setId(1);
        dto.setStemcellUrl("https://bosh-jenkins-artifacts.s3.amazonaws.com/bosh-stemcell/aws/light-bosh-stemcell-2820-aws-xen-hvm-ubuntu-trusty-go_agent.tgz");
        dto.setStemcellName("testStemcellFile");
        dto.setStemcellFileName("light-bosh-stemcell-2820-aws-xen-hvm-ubuntu-trusty-go_agent.tgz");
        dto.setStemcellVersion("2820");
        dto.setStemcellSize("123456789");
        dto.setOsName("UBUNTU");
        dto.setIaasType("AWS");
        dto.setOsVersion("TRUSTY");
        dto.setLight("true");
        if("url".equals(fileType)){
            dto.setFileType("url");
        }else{
            dto.setFileType("version");
        }
        dto.setOverlayCheck("true");
        dto.setCreateUserId("tester");
        dto.setUpdateUserId("tester");
        return dto;
    }

    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : 로컬 File을 통한 스템셀 등록 정보 설정
     * @title : setStemcellRegistInfoFile
     * @return : String
    *****************************************************************/
    public String setStemcellRegistInfoFile(String fileType) throws Exception{
        StemcellManagementDTO.Regist dto = new StemcellManagementDTO.Regist();
        dto.setId(1);
        dto.setStemcellUrl("testurl");
        dto.setStemcellName("testStemcellFile");
        dto.setStemcellFileName("light-bosh-stemcell-2820-aws-xen-hvm-ubuntu-trusty-go_agent.tgz");
        dto.setStemcellVersion("1111");
        dto.setStemcellSize("123456789");
        dto.setOsName("UBUNTU");
        dto.setIaasType("OPENSTACK");
        dto.setOsVersion("TRUSTY");
        dto.setLight("true");
        dto.setFileType(fileType);
        dto.setOverlayCheck("true");
        dto.setCreateUserId("tester");
        dto.setUpdateUserId("tester");
        
        //JSON 형태로 변환
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson= ow.writeValueAsString(dto);
        
        return requestJson;
    }
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : 버전 유형의 스템셀 다운로드 설정
     * @title : setStemcellRegistInfoVersion
     * @return : String
    *****************************************************************/
    private String setStemcellRegistInfoVersion(String fileType) throws Exception {
        StemcellManagementDTO.Regist dto = new StemcellManagementDTO.Regist();
        dto.setId(1);
        dto.setStemcellUrl("testurl");
        dto.setStemcellName("testStemcellName");
        dto.setStemcellFileName("testStecellFileName");
        dto.setStemcellVersion("2820");
        dto.setStemcellSize("123456789");
        dto.setOsName("UBUNTU");
        dto.setIaasType("AWS");
        dto.setOsVersion("TRUSTY");
        dto.setLight("true");
        dto.setFileType(fileType);
        dto.setOverlayCheck("true");
        dto.setCreateUserId("tester");
        dto.setUpdateUserId("tester");
        
        //JSON 형태로 변환
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson= ow.writeValueAsString(dto);
        
        return requestJson;
    }
    
    /****************************************************************
     * @project : Paas 플랫폼 설치 자동화
     * @description : 스템셀 삭제 데이터  설정
     * @title : setStemcellDeleteData
     * @return : String
    *****************************************************************/
    private String setStemcellDeleteData() throws Exception {
        StemcellManagementDTO.Delete dto = new StemcellManagementDTO.Delete();
        dto.setId(1);
        dto.setStemcellFileName("light-bosh-stemcell-2820-aws-xen-hvm-ubuntu-trusty-go_agent.tgz");
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson= ow.writeValueAsString(dto);
        
        return requestJson;
    }

}
