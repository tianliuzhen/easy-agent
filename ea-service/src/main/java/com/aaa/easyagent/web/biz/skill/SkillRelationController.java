package com.aaa.easyagent.web.biz.skill;

import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.SkillBindRequest;
import com.aaa.easyagent.core.domain.request.SkillConfigRequest;
import com.aaa.easyagent.core.domain.request.SkillInstallRequest;
import com.aaa.easyagent.core.domain.request.SkillUnbindRequest;
import com.aaa.easyagent.core.domain.result.SkillConfigResult;
import com.aaa.easyagent.core.service.SkillIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Skill 绑定关系管理控制器
 * 负责 Skill 与 Agent 的绑定/解绑操作
 *
 * @author liuzhen.tian
 * @version 1.0 SkillRelationController.java  2026/4/6
 */
@Slf4j
@RestController
@RequestMapping("eaAgent/skill/")
@RequiredArgsConstructor
public class SkillRelationController {

    private final SkillIntegrationService skillIntegrationService;

    /**
     * 获取用户已安装的 Skill 列表（我的 Skill）
     *
     * @return Skill 配置结果列表
     */
    @GetMapping("getSkillConfigByUserId/")
    public BaseResult getSkillConfigByUserId() {
        try {
            // TODO: 从上下文获取当前用户ID，暂时使用默认值 1
            Long userId = 1L;
            List<SkillConfigResult> userSkills = skillIntegrationService.getUserInstalledSkills(userId);
            return BaseResult.buildSuc(userSkills);
        } catch (Exception e) {
            log.error("获取用户 Skill 配置列表失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 获取官方 Skill 配置列表（user_id = 0）
     *
     * @return 官方 Skill 配置结果列表
     */
    @GetMapping("getOfficialSkills/")
    public BaseResult getOfficialSkills() {
        try {
            List<SkillConfigResult> results = skillIntegrationService.getOfficialSkills();
            return BaseResult.buildSuc(results);
        } catch (Exception e) {
            log.error("获取官方 Skill 配置列表失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 根据分类获取 Skill 列表
     *
     * @param category 技能分类
     * @return Skill 配置结果列表
     */
    @GetMapping("getSkillsByCategory/{category}")
    public BaseResult getSkillsByCategory(@PathVariable String category) {
        try {
            List<SkillConfigResult> results = skillIntegrationService.getSkillsByCategory(category);
            return BaseResult.buildSuc(results);
        } catch (Exception e) {
            log.error("根据分类获取 Skill 列表失败: category={}", category, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 根据 Agent ID 获取已绑定的 Skill 列表
     *
     * @param agentId Agent 标识
     * @return 已绑定的 Skill 配置列表
     */
    @GetMapping("listBoundSkillsByAgentId/{agentId}")
    public BaseResult listBoundSkillsByAgentId(@PathVariable Long agentId) {
        try {
            List<SkillConfigResult> results = skillIntegrationService.getBoundSkillsByAgentId(agentId);
            return BaseResult.buildSuc(results);
        } catch (Exception e) {
            log.error("获取 Agent 绑定的 Skill 列表失败: agentId={}", agentId, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 绑定 Skill 到 Agent
     *
     * @param request 绑定请求
     * @return 绑定结果
     */
    @PostMapping("bind")
    public BaseResult bindSkill(@RequestBody SkillBindRequest request) {
        try {
            Long agentId = Long.valueOf(request.getAgentId());
            Long skillConfigId = request.getSkillConfigId();
            String bindingConfig = request.getBindingConfig();

            boolean success = skillIntegrationService.bindSkillToAgent(agentId, skillConfigId, bindingConfig);
            return BaseResult.buildSuc(success);
        } catch (Exception e) {
            log.error("绑定 Skill 到 Agent 失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 从 Agent 解绑 Skill
     *
     * @param request 解绑请求
     * @return 解绑结果
     */
    @PostMapping("unbind")
    public BaseResult unbindSkill(@RequestBody SkillUnbindRequest request) {
        try {
            Long agentId = Long.valueOf(request.getAgentId());
            Long skillConfigId = request.getSkillConfigId();

            boolean success = skillIntegrationService.unbindSkillFromAgent(agentId, skillConfigId);
            return BaseResult.buildSuc(success);
        } catch (Exception e) {
            log.error("从 Agent 解绑 Skill 失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    // ==================== Skill 用户安装管理接口 ====================

    /**
     * 用户安装 Skill（从市场安装到"我的 Skill"）
     *
     * @param skillConfigId 官方 Skill 配置ID
     * @param customConfig  用户自定义配置（可选）
     * @return 安装结果
     */
    @PostMapping("install")
    public BaseResult installSkill(@RequestBody SkillInstallRequest request) {
        try {
            // TODO: 从上下文获取当前用户ID，暂时使用默认值 1
            Long userId = 1L;
            Long newSkillId = skillIntegrationService.installSkill(userId, request.getSkillConfigId(), request.getCustomConfig());
            return BaseResult.buildSuc(newSkillId);
        } catch (Exception e) {
            log.error("安装 Skill 失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 用户卸载 Skill
     *
     * @param skillConfigId Skill 配置ID
     * @return 卸载结果
     */
    @PostMapping("uninstall/{skillConfigId}")
    public BaseResult uninstallSkill(@PathVariable Long skillConfigId) {
        try {
            // TODO: 从上下文获取当前用户ID，暂时使用默认值 1
            Long userId = 1L;
            boolean success = skillIntegrationService.uninstallSkill(userId, skillConfigId);
            return BaseResult.buildSuc(success);
        } catch (Exception e) {
            log.error("卸载 Skill 失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 检查用户是否已安装指定 Skill
     *
     * @param skillName Skill 名称
     * @return 是否已安装
     */
    @GetMapping("isInstalled/{skillName}")
    public BaseResult isSkillInstalled(@PathVariable String skillName) {
        try {
            // TODO: 从上下文获取当前用户ID，暂时使用默认值 1
            Long userId = 1L;
            boolean installed = skillIntegrationService.isSkillInstalled(userId, skillName);
            return BaseResult.buildSuc(installed);
        } catch (Exception e) {
            log.error("检查 Skill 安装状态失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    // ==================== Skill Server 配置管理接口 ====================

    /**
     * 获取所有 Skill 配置列表
     *
     * @return Skill 配置列表
     */
    @GetMapping("server")
    public BaseResult listAllConfigs() {
        try {
            List<SkillConfigResult> results = skillIntegrationService.getAllSkills();
            return BaseResult.buildSuc(results);
        } catch (Exception e) {
            log.error("获取 Skill 配置列表失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 Skill 配置
     *
     * @param id 配置 ID
     * @return 配置信息
     */
    @GetMapping("server/{id}")
    public BaseResult getConfigById(@PathVariable Long id) {
        try {
            SkillConfigResult result = skillIntegrationService.getSkillConfigById(id);
            return BaseResult.buildSuc(result);
        } catch (Exception e) {
            log.error("获取 Skill 配置失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 新增 Skill 配置
     *
     * @param request 配置请求
     * @return 配置 ID
     */
    @PostMapping("server")
    public BaseResult createConfig(@RequestBody SkillConfigRequest request) {
        try {
            Long id = skillIntegrationService.createSkillConfig(request);
            return BaseResult.buildSuc(id);
        } catch (Exception e) {
            log.error("创建 Skill 配置失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 更新 Skill 配置
     *
     * @param id      配置 ID
     * @param request 配置请求
     * @return 是否成功
     */
    @PutMapping("server/{id}")
    public BaseResult updateConfig(@PathVariable Long id, @RequestBody SkillConfigRequest request) {
        try {
            boolean success = skillIntegrationService.updateSkillConfig(id, request);
            return BaseResult.buildSuc(success);
        } catch (Exception e) {
            log.error("更新 Skill 配置失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 删除 Skill 配置
     *
     * @param id 配置 ID
     * @return 是否成功
     */
    @DeleteMapping("server/{id}")
    public BaseResult deleteConfig(@PathVariable Long id) {
        try {
            boolean success = skillIntegrationService.deleteSkillConfig(id);
            return BaseResult.buildSuc(success);
        } catch (Exception e) {
            log.error("删除 Skill 配置失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }
}
