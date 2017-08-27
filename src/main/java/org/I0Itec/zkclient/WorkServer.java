package org.I0Itec.zkclient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.CreateMode;

/**
 * Created by nevermore on 16/6/22.
 */
public class WorkServer {

    //�ͻ���״̬
    private volatile boolean running = false;

    private ZkClient zkClient;

    //zk���ڵ�·��
    public static final String MASTER_PATH = "/master";

    //����(���ڼ������ڵ�ɾ���¼�)
    private IZkDataListener dataListener;

    //������������Ϣ
    private RunningData serverData;
    //���ڵ������Ϣ
    private RunningData masterData;

    //������
    private ScheduledExecutorService delayExector = Executors.newScheduledThreadPool(1);
    //�ӳ�ʱ��5s
    private int delayTime = 5;



    public WorkServer(RunningData runningData){
        this.serverData = runningData;
        this.dataListener = new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {

            }

            @Override
            public void handleDataDeleted(String s) throws Exception {
                //takeMaster();

                if(masterData != null && masterData.getName().equals(serverData.getName())){//��֮ǰmasterΪ����,����������,�����ӳ�5������(��ֹС����������������ܵ��µ��������ݷ籩)
                    takeMaster();
                }else{
                    delayExector.schedule(new Runnable() {
                        @Override
                        public void run() {
                            takeMaster();
                        }
                    },delayTime, TimeUnit.SECONDS);
                }

            }
        };
    }

    //����
    public void start() throws Exception{
        if(running){
            throw new Exception("server has startup....");
        }
        running = true;
        zkClient.subscribeDataChanges(MASTER_PATH,dataListener);
        takeMaster();
    }

    //ֹͣ
    public void stop() throws Exception{
        if(!running){
            throw new Exception("server has stopped.....");
        }
        running = false;
        delayExector.shutdown();
        zkClient.unsubscribeDataChanges(MASTER_PATH,dataListener);
        releaseMaster();
    }

    //��ע���ڵ�
    private void takeMaster(){
        if(!running) return ;

        try {
            zkClient.create(MASTER_PATH, serverData, CreateMode.EPHEMERAL);
            masterData = serverData;
            System.out.println(serverData.getName()+" is master");

            delayExector.schedule(new Runnable() {//����������,ÿ5s�ͷ�һ�����ڵ�
                @Override
                public void run() {
                    if(checkMaster()){
                        releaseMaster();
                    }
                }
            },5,TimeUnit.SECONDS);


        }catch (ZkNodeExistsException e){//�ڵ��Ѵ���
            RunningData runningData = zkClient.readData(MASTER_PATH,true);
            if(runningData == null){//��ȡ���ڵ�ʱ,���ڵ㱻�ͷ�
                takeMaster();
            }else{
                masterData = runningData;
            }
        } catch (Exception e) {
            // ignore;
        }

    }
    //�ͷ����ڵ�
    private void releaseMaster(){
        if(checkMaster()){
            zkClient.delete(MASTER_PATH);
        }
    }
    //�����Լ��Ƿ������ڵ�
    private boolean checkMaster(){
        try {
            RunningData runningData = zkClient.readData(MASTER_PATH);
            masterData = runningData;
            return masterData.getName().equals(serverData.getName());

        }catch (ZkNoNodeException e){//�ڵ㲻����
            return  false;
        }catch (ZkInterruptedException e){//�����ж�
            return checkMaster();
        }catch (Exception e){//����
            return false;
        }
    }

    public void setZkClient(ZkClient zkClient) {
        this.zkClient = zkClient;
    }

    public ZkClient getZkClient() {
        return zkClient;
    }
}