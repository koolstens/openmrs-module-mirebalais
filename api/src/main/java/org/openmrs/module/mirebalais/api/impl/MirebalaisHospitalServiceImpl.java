/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.mirebalais.api.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.idgen.IdentifierPool;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.RemoteIdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.mirebalais.MirebalaisConstants;
import org.openmrs.module.mirebalais.api.MirebalaisHospitalService;
import org.openmrs.module.mirebalais.api.db.MirebalaisHospitalDAO;
import org.openmrs.module.pihcore.PihCoreConstants;
import org.openmrs.module.pihcore.metadata.core.PatientIdentifierTypes;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link MirebalaisHospitalService}.
 */
public class MirebalaisHospitalServiceImpl extends BaseOpenmrsService implements MirebalaisHospitalService {
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	private MirebalaisHospitalDAO dao;
	
	/**
	 * @param dao the db to set
	 */
	public void setDao(MirebalaisHospitalDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * @return the db
	 */
	public MirebalaisHospitalDAO getDao() {
		return dao;
	}

	/**
	 * @see org.openmrs.module.mirebalais.api.MirebalaisHospitalService#configureZlIdentifierSources()
	 *
	 */
	@Override
	@Transactional
	public void configureZlIdentifierSources() {
		
	}

    @Override
    public SequentialIdentifierGenerator getLocalZlIdentifierGenerator() {
        return getIdentifierSource(PihCoreConstants.LOCAL_ZL_IDENTIFIER_GENERATOR_UUID, SequentialIdentifierGenerator.class);
    }

	@Override
	public IdentifierPool getLocalZlIdentifierPool() {
        return getIdentifierSource(PihCoreConstants.LOCAL_ZL_IDENTIFIER_POOL_UUID, IdentifierPool.class);
	}
	
	@Override
	public RemoteIdentifierSource getRemoteZlIdentifierSource() {
        return getIdentifierSource(PihCoreConstants.REMOTE_ZL_IDENTIFIER_SOURCE_UUID, RemoteIdentifierSource.class);
	}
	
	@Override
	public PatientIdentifierType getZlIdentifierType() {
		return MetadataUtils.existing(PatientIdentifierType.class, PatientIdentifierTypes.ZL_EMR_ID.uuid());
	}

    @Override
    public PatientIdentifierType getExternalDossierIdentifierType() {
		return MetadataUtils.existing(PatientIdentifierType.class, PatientIdentifierTypes.EXTERNAL_DOSSIER_NUMBER.uuid());
    }
	
	@Override
	public SequentialIdentifierGenerator getDossierSequenceGenerator(String identifierSourceUuid) {

		SequentialIdentifierGenerator sequentialIdentifierGenerator = (SequentialIdentifierGenerator) Context.getService(
		    IdentifierSourceService.class).getIdentifierSourceByUuid(
		    identifierSourceUuid);
		
		if (sequentialIdentifierGenerator == null) {
			throw new IllegalStateException("Sequential Identifier Generator For Dossie has not been configured");
		}
		
		return sequentialIdentifierGenerator;
	}

	@Override
	public PatientIdentifierType getDossierIdentifierType() {
		return MetadataUtils.existing(PatientIdentifierType.class, PatientIdentifierTypes.DOSSIER_NUMBER.uuid());
	}

    /**
     * @see org.openmrs.api.OrderService#getNextRadiologyOrderNumberSeedSequenceValue()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized Long getNextRadiologyOrderNumberSeedSequenceValue() {
        return dao.getNextRadiologyOrderNumberSeedSequenceValue();
    }

    private <T extends IdentifierSource> T getIdentifierSource(String uuid, Class<T> sourceType) {
        IdentifierSourceService iss = Context.getService(IdentifierSourceService.class);
        IdentifierSource source = iss.getIdentifierSourceByUuid(uuid);
        if (source == null) {
            throw new IllegalStateException(sourceType.getSimpleName() + " has not been configured");
        }
        return (T) source;
    }
	
}
