package com.duowei.kitchen_china.fragment;


import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.duowei.kitchen_china.R;
import com.duowei.kitchen_china.adapter.RecAdapter;
import com.duowei.kitchen_china.adapter.SpacesItemDecoration;
import com.duowei.kitchen_china.bean.Cfpb;
import com.duowei.kitchen_china.bean.Cfpb_item;
import com.duowei.kitchen_china.event.StartProgress;
import com.duowei.kitchen_china.httputils.Net;
import com.duowei.kitchen_china.httputils.Post;
import com.duowei.kitchen_china.print.PrintHandler;
import com.duowei.kitchen_china.uitls.DateTimes;

import org.greenrobot.eventbus.EventBus;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment implements RecAdapter.onItemClickListener, RecAdapter.onContinueClickListener {

    private RecAdapter mRecAdapter;
    private List<Cfpb> listCfpb;
    private List<Cfpb> listCfpbComplete;//己完成的
    private int tempList;
    private int currentPosition=0;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View inflate = inflater.inflate(R.layout.fragment_main, container, false);
        listCfpb=new ArrayList<>();
        listCfpbComplete =new ArrayList<>();
        RecyclerView rv = (RecyclerView) inflate.findViewById(R.id.recycleView);
        rv.setLayoutManager(new GridLayoutManager(getActivity(),4));//gridview布局,4列
        rv.addItemDecoration(new SpacesItemDecoration(5));//设置item边距
        rv.setItemAnimator(new DefaultItemAnimator());
        mRecAdapter = new RecAdapter(getActivity(), listCfpb);
        rv.setAdapter(mRecAdapter);
        mRecAdapter.setOnItemClickListener(this);
        mRecAdapter.setOnContinueClickListener(this);
        return inflate;
    }
    public void setRecycleView(List<Cfpb>list){
        mRecAdapter.setList(listCfpb=list);
        if(list.size()==tempList||list.size()==0){
            mRecAdapter.notifyDataSetChanged();
        }else if(list.size()<tempList){//list变小，启用删除动画
            mRecAdapter.notifyItemRemoved(currentPosition);
            mRecAdapter.notifyItemRangeChanged(currentPosition,mRecAdapter.getItemCount());
        }else if(list.size()>tempList){//list变大，启用增加动画
            mRecAdapter.notifyItemInserted(list.size()-1);
        }
        tempList=list.size();
    }

    public void setRecycleView2(List<Cfpb>list){
        mRecAdapter.setList(listCfpb=list);
        mRecAdapter.notifyDataSetChanged();
    }

    public void updateSuccess(){
        Post.getInstance().postCfpb(Net.sql_cfpb);
        DataSupport.saveAll(listCfpbComplete);
        PrintHandler.getInstance().print(listCfpbComplete);
    }

    @Override
    public void setOnItemClickListener(int index) {
        mRecAdapter.setIndex(index);
        mRecAdapter.notifyDataSetChanged();
    }
    /**继续按键点击事件*/
    @Override
    public void setOnContinueClickListener(int index,float num) {
        currentPosition=index;

        float tempNum=0;
        Cfpb cfpb21=null;
        listCfpbComplete.clear();
        EventBus.getDefault().post(new StartProgress());
        String sql="";
        mRecAdapter.setIndex(index);
        Cfpb cfpb = listCfpb.get(index);
        List<Cfpb_item> listCfpb = cfpb.getListCfpb();
        for(int i=0;i<listCfpb.size();i++){
            if(num>0){
                Cfpb_item cfpbItem = listCfpb.get(i);
                if(cfpbItem.sl1<=num){//当前桌号待删除的单品数量<=num,直接删除这行
                    sql+="delete from cfpb where xh='"+cfpbItem.xh+"'|";
                    tempNum=cfpbItem.sl1;
                }else {//当前桌号待删除的单品数量>num,更新己用数量字段
                    sql+="update cfpb set ywcsl=isnull(ywcsl,0)+"+num+" where xh='"+cfpbItem.xh+"'|";
                    tempNum=num;
                }
                //己完成
                cfpb21 = new Cfpb(cfpb.getXH(), cfpb.getXmbh(), cfpb.getXmmc(), cfpb.getDw(),
                            cfpbItem.sl1, cfpbItem.pz, cfpb.getXdsj(), cfpbItem.czmc1,
                            cfpbItem.fzs, cfpb.getYhmc(), tempNum, DateTimes.getTime(),
                        DateTimes.getTime2(cfpb.getXdsj()));
                listCfpbComplete.add(cfpb21);
                num=num-cfpbItem.sl1;
            }else{
                break;
            }
        }
        Post.getInstance().setPost7(sql);
    }
}
