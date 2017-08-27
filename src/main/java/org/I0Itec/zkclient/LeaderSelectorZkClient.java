package org.I0Itec.zkclient;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nevermore on 16/6/23.
 */
public class LeaderSelectorZkClient {

    //�����ķ������
    private static final int        CLIENT_QTY = 10;
    //zookeeper�������ĵ�ַ
    private static final String     ZOOKEEPER_SERVER = "192.168.1.107:2181";


    public static void main(String[] args) throws Exception{
        //��������zkClient���б�
        List<ZkClient> clients = new ArrayList<ZkClient>();
        //�������з�����б�
        List<WorkServer>  workServers = new ArrayList<WorkServer>();

        try{
            for ( int i = 0; i < CLIENT_QTY; ++i ){
                //����zkClient
                ZkClient client = new ZkClient(ZOOKEEPER_SERVER, 5000, 5000, new SerializableSerializer());
                clients.add(client);
                //����serverData
                RunningData runningData = new RunningData();
                runningData.setCid(Long.valueOf(i));
                runningData.setName("Client #" + i);
                //��������
                WorkServer  workServer = new WorkServer(runningData);
                workServer.setZkClient(client);

                workServers.add(workServer);
                workServer.start();
            }

            System.out.println("�ûس����˳���\n");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        }finally{
            System.out.println("Shutting down...");

            for ( WorkServer workServer : workServers ){
                try {
                    workServer.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for ( ZkClient client : clients ){
                try {
                    client.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}