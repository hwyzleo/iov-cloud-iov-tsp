package net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportIdcmCmd {

    private String batchNum;

    private String supplierCode;

    private List<IdcmImportCmd> idcmList;

}