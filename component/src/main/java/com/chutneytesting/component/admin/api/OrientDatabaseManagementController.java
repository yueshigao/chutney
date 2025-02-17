package com.chutneytesting.component.admin.api;

import com.chutneytesting.server.core.domain.admin.DatabaseAdminService;
import com.chutneytesting.server.core.domain.admin.SqlResult;
import com.chutneytesting.server.core.domain.tools.PaginatedDto;
import com.chutneytesting.server.core.domain.tools.PaginationRequestWrapperDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/database")
@CrossOrigin(origins = "*")
public class OrientDatabaseManagementController {

    private final DatabaseAdminService orientAdminService;

    OrientDatabaseManagementController(DatabaseAdminService orientAdminService) {
        this.orientAdminService = orientAdminService;
    }

    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    @PostMapping("/execute/orient")
    public SqlResult executeOrient(@RequestBody String query) {
        return orientAdminService.execute(query);
    }


    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    @PostMapping("/paginate/orient")
    public PaginatedDto<SqlResult> executeOrient(@RequestBody PaginationRequestWrapperDto<String> paginationRequestWrapperDto) {
        return orientAdminService.paginate(paginationRequestWrapperDto);
    }

}
