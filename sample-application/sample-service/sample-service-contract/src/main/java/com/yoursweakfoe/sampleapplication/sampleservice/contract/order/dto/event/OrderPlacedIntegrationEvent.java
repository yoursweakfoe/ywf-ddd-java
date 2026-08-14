package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.event;

import com.yoursweakfoe.common.contract.dto.Event;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 璁㈠崟宸蹭笅鍗曢泦鎴愪簨浠讹紙璺ㄦ湇鍔￠€氱煡锛夈€? *
 * <p>鐢?Application 灞傜洃鍚?Domain Event锛圤rderPlacedEvent锛夊悗杞崲骞跺彂甯冨埌 MQ銆? * 娑堣垂鏂癸紙鍏朵粬寰湇鍔★級閫氳繃璁㈤槄 MQ Topic 鎺ユ敹姝や簨浠躲€? *
 * <p>娉ㄦ剰锛氳繖鏄?Integration Event锛堝澶栧绾︼級锛屼笉鏄?Domain Event锛堥鍩熷唴閮級銆? * Domain Event 瀹氫箟鍦?{@code domain/order/event/OrderPlacedEvent}锛屽寘鍚洿涓板瘜鐨勯鍩熺粏鑺傘€? */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedIntegrationEvent implements Event, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 璁㈠崟 ID */
    private String orderId;

    /** 瀹㈡埛 ID */
    private String customerId;
}
