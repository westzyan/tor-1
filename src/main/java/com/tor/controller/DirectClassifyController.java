package com.tor.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tor.domain.Flow;
import com.tor.domain.Model;
import com.tor.domain.Packet;
import com.tor.result.CodeMsg;
import com.tor.result.Const;
import com.tor.result.Result;
import com.tor.service.ModelService;
import com.tor.service.TestService;
import com.tor.service.PacketService;
import com.tor.util.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

@Controller
//@RequestMapping("/classify")
@Slf4j
public class DirectClassifyController {

    @Autowired
    TestService testService;
    @Autowired
    private PacketService packetService;
    @Autowired
    private ModelService modelService;
    private Model model = new Model();

    @RequestMapping(method = RequestMethod.GET)
    public String findAll(ModelMap map, @RequestParam(required = false, defaultValue = "1", value = "pn") Integer pn, @RequestParam(required = false, defaultValue = "1", value = "pn1") Integer pn1) {
        List<Packet> packetList = new LinkedList<>();
        PageHelper.startPage(pn, 6);
        packetList = packetService.findAllTestPacket();
        map.addAttribute("packetList", packetList);
        PageInfo<Packet> packetPage = new PageInfo<>(packetList);
        map.addAttribute("packetPage", packetPage);
        return Const.CLASSIFY_PAGE;
    }

    /**
     * @param testCsvPath
     * @param modelMap
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    public String test(@RequestParam("testFile") String testCsvPath, ModelMap modelMap) throws Exception {
        if (testCsvPath == null) {
            modelMap.addAttribute("result", Result.error(CodeMsg.NULL_DATA));
            return Const.CLASSIFY_PAGE;
        }
        //对testCsvPath进行处理，得到测试文件名字
        String testFileName = testCsvPath.substring(testCsvPath.lastIndexOf("/")).replace("/", "");
        //选取最新的模型用于分分类
        model = modelService.findLastModel();
        if (model == null) {
            modelMap.addAttribute("result", Result.error(CodeMsg.NULL_DATA));
            return Const.CLASSIFY_PAGE;
        } else {
            String modelPath = model.getModelPath();//.model
            String featurePath = model.getFeaturePath();//Feature.txt
            //调用测试算法，得到一个表，表示测试结果。
            List<Flow> resultList = testService.getModelClassifyList(testFileName, testCsvPath, modelPath, featurePath);
            modelMap.addAttribute("resultList", resultList);
            return Const.TEST_RESULT_PAGE;
        }
    }

    //todo 无法删除文件
    //删除文件
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public String deletePacket(@PathVariable Integer id, ModelMap modelMap) {
        packetService.deletePacket(id);
        List<Packet> resList = packetService.findAllTestPacket();
        PageInfo<Packet> pageList = new PageInfo<>(resList);
        modelMap.addAttribute("data", resList);
        modelMap.addAttribute("page", pageList);
        return Const.TEST_PAGE;
    }

    //todo 无法添加？
    @RequestMapping(value = "/addPacket")
    public String addPacket(ModelMap modelMap, @RequestParam("file") MultipartFile file, @RequestParam("packet") String type) throws Exception {
        try {
            if (file.isEmpty()) {
                modelMap.addAttribute("result", Result.error(CodeMsg.NULL_DATA));
                return Const.TEST_PAGE;
            }
            String filePcapName = file.getOriginalFilename();
            String suffixName = filePcapName.substring(filePcapName.lastIndexOf("."));
            if (!".pcap".equals(suffixName)) {
                modelMap.addAttribute("result", Result.error(CodeMsg.INVIVAD_FILE));
                return Const.TEST_PAGE;
            }
            //path为要保存的pcap地址拼接原始fileName
            String fullPcapName = PropertiesUtil.getPcapPath() + filePcapName;
            File fullPcapFile = new File(fullPcapName);
            //检测是否存在目标
            if (!fullPcapFile.getParentFile().exists()) {
                fullPcapFile.getParentFile().mkdirs();
            }
            //TODO pacp转换为csv文件 去掉.pcap，以.csv结尾
            if (!fullPcapFile.exists()) {
                Packet packet = new Packet();
                packet.setPacketName(filePcapName);
                packet.setPacketPath(fullPcapName);
                packet.setType(type);
                packet.setCsvPath(PropertiesUtil.getPcapCsvPath() + filePcapName.replace(".pcap", ""));
                packetService.insertPacket(packet);
                file.transferTo(fullPcapFile);
            } else {

            }

        } catch (Exception e) {
            log.error(e.toString());
        }
        //加入数据包之后，显示现有数据包
        List<Packet> packetList = packetService.findAllTestPacketDesc();
        PageInfo<Packet> pageList = new PageInfo<>(packetList);
        modelMap.addAttribute("data", packetList);
        modelMap.addAttribute("page", pageList);
        return Const.TEST_PAGE;
    }
}
