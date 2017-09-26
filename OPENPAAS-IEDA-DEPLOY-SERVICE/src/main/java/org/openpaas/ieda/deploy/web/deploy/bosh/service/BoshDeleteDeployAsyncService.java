package org.openpaas.ieda.deploy.web.deploy.bosh.service;

import java.security.Principal;
import java.util.Arrays;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.openpaas.ieda.common.exception.CommonException;
import org.openpaas.ieda.common.web.security.SessionInfoDTO;
import org.openpaas.ieda.deploy.api.director.utility.DirectorRestHelper;
import org.openpaas.ieda.deploy.web.config.setting.dao.DirectorConfigVO;
import org.openpaas.ieda.deploy.web.config.setting.service.DirectorConfigService;
import org.openpaas.ieda.deploy.web.deploy.bosh.dao.BoshDAO;
import org.openpaas.ieda.deploy.web.deploy.bosh.dao.BoshVO;
import org.openpaas.ieda.deploy.web.deploy.bosh.dto.BoshParamDTO;
import org.openpaas.ieda.deploy.web.deploy.common.dao.network.NetworkDAO;
import org.openpaas.ieda.deploy.web.deploy.common.dao.resource.ResourceDAO;
import org.openpaas.ieda.deploy.web.management.code.dao.CommonCodeDAO;
import org.openpaas.ieda.deploy.web.management.code.dao.CommonCodeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BoshDeleteDeployAsyncService {
	
	@Autowired private SimpMessagingTemplate messagingTemplate;
	@Autowired private DirectorConfigService directorConfigService;
	@Autowired private BoshDAO boshDao;
	@Autowired private NetworkDAO networkDao;
	@Autowired private ResourceDAO resourceDao;
	@Autowired private CommonCodeDAO commonCodeDao;
	
	final private static String PARENT_CODE="1000"; //배포 코드
	final private static String SUB_GROUP_CODE="1200";
	final private static String STATUS_SUB_GROUP_CODE="1200"; //배포 상태 코드
	final private static String CODE_NAME="DEPLOY_TYPE_BOSH"; //배포 할 플랫폼명
	final private static String MESSAGE_ENDPOINT = "/deploy/bosh/delete/logs"; 
	
	/***************************************************
	 * @project          : Paas 플랫폼 설치 자동화
	 * @description   : Bosh에 플랫폼 삭제 요청
	 * @title               : deleteDeploy
	 * @return            : void
	***************************************************/
	public void deleteDeploy(BoshParamDTO.Delete dto, Principal principal) {
		
		BoshVO vo = null;
		String deploymentName = null;
		CommonCodeVO commonCode = null;
		SessionInfoDTO sessionInfo = new SessionInfoDTO(principal);
		
		vo = boshDao.selectBoshDetailInfo(Integer.parseInt(dto.getId()));
		if ( vo != null ) deploymentName = vo.getDeploymentName();
		if ( StringUtils.isEmpty(deploymentName) ) {
			throw new CommonException("notfound.boshdelete.exception",
					"배포정보가 존재하지 않습니다..", HttpStatus.NOT_FOUND);
		}
		DirectorConfigVO defaultDirector = directorConfigService.getDefaultDirector();
		
		if ( vo != null ) {
			commonCode = commonCodeDao.selectCommonCodeByCodeName(PARENT_CODE, STATUS_SUB_GROUP_CODE, "DEPLOY_STATUS_DELETING");
			if( commonCode != null ){
				vo.setDeployStatus(commonCode.getCodeValue());
				vo.setUpdateUserId(sessionInfo.getUserId());
				saveDeployStatus(vo);
			}
		}
		
		try {
			HttpClient httpClient = DirectorRestHelper.getHttpClient(defaultDirector.getDirectorPort());
			
			DeleteMethod deleteMethod = new DeleteMethod(DirectorRestHelper.getDeleteDeploymentURI(defaultDirector.getDirectorUrl(), defaultDirector.getDirectorPort(), deploymentName));
			deleteMethod = (DeleteMethod)DirectorRestHelper.setAuthorization(defaultDirector.getUserId(), defaultDirector.getUserPassword(), (HttpMethodBase)deleteMethod);
			int statusCode = httpClient.executeMethod(deleteMethod);
			if ( statusCode == HttpStatus.MOVED_PERMANENTLY.value()
			  || statusCode == HttpStatus.MOVED_TEMPORARILY.value()	) {
				
				Header location = deleteMethod.getResponseHeader("Location");
				String taskId = DirectorRestHelper.getTaskId(location.getValue());
				
				DirectorRestHelper.trackToTask(defaultDirector, messagingTemplate, MESSAGE_ENDPOINT, httpClient, taskId, "event", principal.getName());
				//정보 삭제
				deleteBoshInfo(vo);
			} else {
				DirectorRestHelper.sendTaskOutput(principal.getName(), messagingTemplate, MESSAGE_ENDPOINT, "done", Arrays.asList("Bosh 삭제가 완료되었습니다."));
				deleteBoshInfo(vo);
			}
		}catch(RuntimeException e){
			DirectorRestHelper.sendTaskOutput(principal.getName(), messagingTemplate, MESSAGE_ENDPOINT, "error", Arrays.asList("배포삭제 중 Exception이 발생하였습니다."));
			commonCode = commonCodeDao.selectCommonCodeByCodeName(PARENT_CODE, SUB_GROUP_CODE, "DEPLOY_STATUS_FAILED");
			vo.setDeployStatus(commonCode.getCodeName());
			saveDeployStatus(vo);
		}catch ( Exception e) {
			DirectorRestHelper.sendTaskOutput(principal.getName(), messagingTemplate, MESSAGE_ENDPOINT, "error", Arrays.asList("배포삭제 중 Exception이 발생하였습니다."));
			commonCode = commonCodeDao.selectCommonCodeByCodeName(PARENT_CODE, SUB_GROUP_CODE, "DEPLOY_STATUS_FAILED");
			vo.setDeployStatus(commonCode.getCodeName());
			saveDeployStatus(vo);
		}
	}
	
	/***************************************************
	 * @project          : Paas 플랫폼 설치 자동화
	 * @description   : Bosh 데이터 삭제
	 * @title               : deleteBoshInfo
	 * @return            : void
	***************************************************/
	@Transactional
	public void deleteBoshInfo( BoshVO vo ){
		if ( vo != null ) {
			boshDao.deleteBoshInfoRecord(vo.getId());
			CommonCodeVO codeVo = commonCodeDao.selectCommonCodeByCodeName(PARENT_CODE, SUB_GROUP_CODE, CODE_NAME);
			networkDao.deleteNetworkInfoRecord( vo.getId(), codeVo.getCodeName() );
			resourceDao.deleteResourceInfo( vo.getId(), codeVo.getCodeName() );	
		}
	}
	
	/***************************************************
	 * @project          : Paas 플랫폼 설치 자동화
	 * @description   : Bosh 배포 상태 저장
	 * @title               : saveDeployStatus
	 * @return            : BoshVO
	***************************************************/
	public BoshVO saveDeployStatus(BoshVO boshVo) {
		if ( boshVo == null ) return null;
		boshDao.updateBoshInfo(boshVo);
		return boshVo;
	}

	/***************************************************
	 * @project          : Paas 플랫폼 설치 자동화
	 * @description   : 비동기식으로 deleteDeploy 메소드 호출
	 * @title               : deleteDeployAsync
	 * @return            : void
	***************************************************/
	@Async
	public void deleteDeployAsync(BoshParamDTO.Delete dto, Principal principal) {
		deleteDeploy(dto, principal);
	}	
}
