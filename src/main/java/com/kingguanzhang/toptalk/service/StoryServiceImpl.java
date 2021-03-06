package com.kingguanzhang.toptalk.service;

import com.kingguanzhang.toptalk.entity.Story;
import com.kingguanzhang.toptalk.repositories.StoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

//@CacheConfig(cacheNames = "story")
@Service
public class StoryServiceImpl {

    @Autowired
    private StoryRepository storyRepository;



    /**
     * 自定义的查询方法,通过关键字和状态分页查询所有story;
     * @param keyword
     * @param status
     * @param pageable
     * @return
     */
    public Page<Story> findByKeywordAndStatus(String keyword,Integer status,Pageable pageable){
        Page<Story> storyPage = storyRepository.findByKeywordAndStatus(keyword,status,pageable);
        return storyPage;
    }
    
    
    /**
     * 分页查询所有;
     * @return
     */
    //@Cacheable(value = "story",key = "getMethodName()+'['+#a0.pageNumber+']'+'['+#a0.pageSize+']'+'['+#a0.sort+']'")
    public Page<Story> findAll(Pageable pageable){
        Page<Story> page;
        try {
            page = storyRepository.findAll(pageable);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("查询数据库字段时出现异常");
        }

        return  page;
    }


    /**
     * 通过id查询单个;
     * @param id
     * @return
     */
    //@Cacheable(value = "story",key = "getMethodName()+'['+#a0+']'")
    public Story findById(Long id){
        Optional<Story> temp = storyRepository.findById(id);
        if (temp.isPresent()) {
            return temp.get();
        }else {
            return null;
        }
    }

    /**
     * 通过Example例子查询单个;
     * @param example
     * @return
     */
    public Story findOne(Example<Story> example){
        Optional<Story> temp = storyRepository.findOne(example);
        if (temp.isPresent()) {
            return temp.get();
        }else {
            return null;
        }
    }

    /**
     * 通过Example查询所有;
     * @param example
     * @param pageable
     * @return
     */
    public Page<Story> findAllByExample(Example<Story> example, Pageable pageable){
        Page<Story> page = storyRepository.findAll(example, pageable);
        return page;
    }

    /**
     * 通过id查询所有;
     * @param list
     * @return
     */
    public List<Story> findAllById(List<Long> list){
        List<Story> allById = storyRepository.findAllById(list);
        return allById;
    }


    /**
     * 持久化单条数据;
     * @param object
     */
    //@CacheEvict(value = "story" )
    public void save(Story object){
        if (null == object){
            throw new RuntimeException("传入的参数不能为空");
        }
        //因为long类型的id默认初始值是0,此处需要重置为空才能防止在数据库表中重复;
        Long id=null;
        try {
            storyRepository.save(object);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("更新数据库字段时出现异常");
        }
    }
    /**
     * 持久化并返回id;
     * @param object
     */
    //@CacheEvict(value = "story" )
    public long saveAndFlush(Story object){
        if (null == object){
            throw new RuntimeException("传入的参数不能为空");
        }
        Long id=null;
        try {
            Story temp = storyRepository.saveAndFlush(object);
            id = temp.getId();
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("更新数据库字段时出现异常");
        }
        return id;
    }

    /**
     * 持久化所有;
     * @param list
     */
    //@CacheEvict(value = "story" )
    public void saveAll(List<Story> list){
        if (null == list || 0 == list.size()){
            throw new RuntimeException("传入的参数不能为空");
        }
        Long id=null;
        try {
            storyRepository.saveAll(list);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("持久化数据库字段时出现异常");
        }
    }

    /**
     * 通过Id删除单条记录;
     * @param id
     */
    //@CacheEvict(value = "story" )
    public void delete(Long id){
        if (null == id){
            throw new RuntimeException("传入的参数不能为空");
        }
        try{
            storyRepository.deleteById(id);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("删除数据库字段时出现异常");
        }
    }

    /**
     * 删除所有;
     * @param list
     */
    //@CacheEvict(value = "story" )
    public void deleteAll(List<Story> list){
        if (null == list || 0 == list.size()){
            throw new RuntimeException("传入的参数不能为空");
        }
        try{
            storyRepository.deleteAll(list);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("删除数据库字段时出现异常");
        }
    }

    /**
     * 统计总数;
     * @return
     */
    public Long count(){
        long count = storyRepository.count();
        return count;
    }

    /**
     * 通过样子统计;
     * @param example
     * @return
     */
    public Long countByExample(Example<Story> example){
        long count = storyRepository.count(example);
        return count;
    }
}
