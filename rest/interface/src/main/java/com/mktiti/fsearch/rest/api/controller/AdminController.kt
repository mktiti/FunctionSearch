package com.mktiti.fsearch.rest.api.controller

import com.mktiti.fsearch.dto.SearchStatistics
import com.mktiti.fsearch.rest.api.AdminOnly
import com.mktiti.fsearch.rest.api.handler.AdminHandler
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("\${api.base.path}")
@CrossOrigin("\${cross.origin}")
@Tag(name = "admin")
class AdminController(private val handler: AdminHandler) {

    @GetMapping("/statistics/search")
    @ResponseBody
    @AdminOnly
    fun searchStats(): SearchStatistics = handler.searchStats()

}