package com.zxl.chatbase.wroboot.webhook.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;
import java.util.Date;

/**
 * 值班群聊表
 */
@Data
@TableName("t_duty_chat_group") // 对应数据库表名
public class DutyChatGroup implements Serializable {

    @TableId(value = "rec_id", type = IdType.AUTO)
    private Integer recId;

    @TableField("chat_group_url_id")
    @Size(max = 255, message = "值班群chatId不能超过255个字符")
    private String chatGroupUrlId;

    @TableField("chat_group_name")
    @NotEmpty(message = "值班群群名不能为空")
    @Size(max = 20, message = "值班群群名不能超过20个字符")
    private String chatGroupName;

    @TableField("distribution_robot_url")
    @NotEmpty(message = "值班群分发机器人链接不能为空")
    @Size(max = 255, message = "值班群分发机器人链接不能超过255个字符")
    private String distributionRobotUrl;

    @TableField("is_disabled")
    private Boolean  isDisabled;

    @TableField("version")
    @Version
    private Integer version;

    @Min(value = 0, message = "总共值班天数不能小于0")
    @TableField("cycle_span")
    private Integer cycleSpan;

    @TableField("is_create_tapd")
    private Boolean  isCreateTapd;

    /**
     * 与created字段相等说明被该记录被创建或被编辑之后第一次分发任务
     */
    @TableField("sentinel_start_time")
    private Date sentinelStartTime;

    //@TableField(fill = FieldFill.INSERT)
    private Date created;

    //@TableField(fill = FieldFill.INSERT_UPDATE)
    private Date modified;

    //创建人id
    private Integer creatorId;

    @TableField(exist = false)
    private String creatorName;

    @TableField("duty_send_ids")
    @NotEmpty(message = "值班群消息分发人不能为空")
    @Size(max = 1024, message = "值班群消息发送人人数过多")
    private String dutySendIds;

    @TableField("duty_receive_ids")
    @NotEmpty(message = "值班群消息接收人不能为空")
    @Size(max = 1024, message = "值班群消息接收人人数过多")
    private String dutyReceiveIds;

    @TableField("intell_robot_ids")
    @NotEmpty(message = "智能机器人不能为空")
    @Size(max = 1024, message = "智能机器人过多")
    private String intellRobotIds;

    @TableField(exist = false)
    private int dutyType;

    @TableField(exist = false)
    @NotEmpty(message = "值班群消息发送人不能为空")
    @Size(max = 1024, message = "值班群消息发送人人数过多")
    private List<Integer> dutySendIdsList;

    @TableField(exist = false)
    @NotEmpty(message = "值班群消息接收人不能为空")
    @Size(max = 1024, message = "值班群消息接收人人数过多")
    private List<Integer> dutyReceiveIdsList;

    @TableField(exist = false)
    @NotEmpty(message = "智能机器人不能为空")
    @Size(max = 1024, message = "智能机器人过多")
    private List<Integer> intellRobotIdsList;

 }
