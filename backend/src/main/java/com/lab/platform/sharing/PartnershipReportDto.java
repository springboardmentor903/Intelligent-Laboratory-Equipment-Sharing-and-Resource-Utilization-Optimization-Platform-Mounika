package com.lab.platform.sharing;

import java.util.ArrayList;
import java.util.List;

public class PartnershipReportDto {

    private PartnerSummaryDto partner;
    private List<ItemizedFeeItemDto> sharedSessions = new ArrayList<>();
    private List<MostSharedEquipmentDto> equipmentProvided = new ArrayList<>();
    private List<MostSharedEquipmentDto> equipmentConsumed = new ArrayList<>();

    public PartnershipReportDto() {}

    public PartnerSummaryDto getPartner() {
        return partner;
    }

    public void setPartner(PartnerSummaryDto partner) {
        this.partner = partner;
    }

    public List<ItemizedFeeItemDto> getSharedSessions() {
        return sharedSessions;
    }

    public void setSharedSessions(List<ItemizedFeeItemDto> sharedSessions) {
        this.sharedSessions = sharedSessions;
    }

    public List<MostSharedEquipmentDto> getEquipmentProvided() {
        return equipmentProvided;
    }

    public void setEquipmentProvided(List<MostSharedEquipmentDto> equipmentProvided) {
        this.equipmentProvided = equipmentProvided;
    }

    public List<MostSharedEquipmentDto> getEquipmentConsumed() {
        return equipmentConsumed;
    }

    public void setEquipmentConsumed(List<MostSharedEquipmentDto> equipmentConsumed) {
        this.equipmentConsumed = equipmentConsumed;
    }
}
