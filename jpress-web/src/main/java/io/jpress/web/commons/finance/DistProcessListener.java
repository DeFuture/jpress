/**
 * Copyright (c) 2016-2019, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.web.commons.finance;

import com.jfinal.aop.Inject;
import com.jfinal.log.Log;
import io.jpress.core.finance.OrderFinishedListener;
import io.jpress.model.UserAmountStatement;
import io.jpress.model.UserOrderItem;
import io.jpress.service.UserAmountStatementService;
import io.jpress.service.UserService;

import java.math.BigDecimal;


public class DistProcessListener implements OrderFinishedListener {

    public static final Log LOG = Log.getLog(DistProcessListener.class);

    @Inject
    private UserService userService;

    @Inject
    private UserAmountStatementService statementService;


    @Override
    public void onFinished(UserOrderItem orderItem) {
        BigDecimal distAmount = orderItem.getDistAmount().multiply(BigDecimal.valueOf(orderItem.getProductCount()));

        if (orderItem.isFinished() //交易结束，用户不能申请退款
                && orderItem.getDistUserId() != null //分销用户不能为空
                && orderItem.getBuyerId() != null  //支付用户不能为空
                && orderItem.getDistUserId().equals(orderItem.getBuyerId()) //分销用户和支付用户不能是同一个人
                && orderItem.getPayAmount() != null //支付金额不能为空
                && orderItem.getPayAmount().compareTo(BigDecimal.ZERO) > 0 //支付金额必须大于0
                && orderItem.getDistAmount() != null //分销金额不能为空
                && orderItem.getPayAmount().compareTo(distAmount) > 0 //支付金额必须大于分销金额
        ) {

            BigDecimal userAmount = userService.queryUserAmount(orderItem.getDistUserId());

            //更新用于余额
            if (userService.updateUserAmount(orderItem.getDistUserId(), userAmount,
                    distAmount)) {

                UserAmountStatement statement = new UserAmountStatement();
                statement.setUserId(orderItem.getDistUserId());
                statement.setActionDesc(UserAmountStatement.ACTION_DIST);
                statement.setActionName("分销收入");
                statement.setActionDesc("分销收入");
                statement.setActionRelativeType("user_order_item");
                statement.setActionRelativeId(orderItem.getId());
                statement.setOldAmount(userAmount);
                statement.setNewAmount(userAmount.add(distAmount));
                statement.setChangeAmount(distAmount);

                statementService.save(statement);
            }
        }
    }
}
