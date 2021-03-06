/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.pod.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.core.exception.DataException;
import org.openlmis.core.service.ProductService;
import org.openlmis.db.categories.UnitTests;
import org.openlmis.fulfillment.shared.FulfillmentPermissionService;
import org.openlmis.order.domain.Order;
import org.openlmis.order.domain.OrderStatus;
import org.openlmis.order.service.OrderService;
import org.openlmis.pod.domain.OrderPOD;
import org.openlmis.pod.repository.PODRepository;
import org.openlmis.rnr.builder.RequisitionBuilder;
import org.openlmis.rnr.domain.Rnr;
import org.openlmis.rnr.service.RequisitionService;
import org.openlmis.shipment.domain.ShipmentLineItem;
import org.openlmis.shipment.service.ShipmentService;

import java.text.ParseException;
import java.util.List;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.openlmis.core.domain.Right.MANAGE_POD;
import static org.openlmis.order.domain.OrderStatus.*;

@Category(UnitTests.class)
@RunWith(MockitoJUnitRunner.class)
public class PODServiceTest {

  @Mock
  private PODRepository repository;

  @Mock
  private OrderService orderService;

  @Mock
  private ProductService productService;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private ShipmentService shipmentService;

  @Mock
  private FulfillmentPermissionService fulfillmentPermissionService;

  @InjectMocks
  private PODService podService;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Long podId;
  private Long orderId;
  private Long userId;
  private OrderPOD orderPod;

  @Before
  public void setUp() throws Exception {
    podService = spy(podService);
    podId = 1L;
    orderId = 2L;
    userId = 3L;
    orderPod = new OrderPOD(podId);
    orderPod.setOrderId(orderId);
    orderPod.setCreatedBy(userId);
  }

  @Test
  public void shouldCreatePODFromPackedOrder() throws Exception {
    long orderId = 4L;
    OrderPOD orderPOD = spy(new OrderPOD(orderId, 8L));

    doNothing().when(podService).checkPermissions(orderPOD);
    when(orderService.hasStatus(orderId, PACKED)).thenReturn(true);
    List<ShipmentLineItem> shipmentLineItems = asList(mock(ShipmentLineItem.class));
    when(shipmentService.getLineItems(orderId)).thenReturn(shipmentLineItems);
    Rnr requisition = make(a(RequisitionBuilder.defaultRequisition));
    when(requisitionService.getFullRequisitionById(orderId)).thenReturn(requisition);
    doNothing().when(orderPOD).fillPOD(requisition);
    doNothing().when(orderPOD).fillPODLineItems(shipmentLineItems);
    when(repository.insert(orderPOD)).thenReturn(orderPOD);

    OrderPOD pod = podService.createPOD(orderPOD);

    verify(podService).checkPermissions(orderPOD);
    verify(orderPOD).fillPOD(requisition);
    verify(orderPOD).fillPODLineItems(shipmentLineItems);
    assertThat(pod, is(orderPOD));
  }

  @Test
  public void shouldCreatePODFromReleasedOrder() throws Exception {
    long orderId = 6L;
    OrderPOD orderPOD = spy(new OrderPOD(orderId, 8L));

    doNothing().when(podService).checkPermissions(orderPOD);
    when(orderService.hasStatus(orderId, RELEASED, READY_TO_PACK, TRANSFER_FAILED)).thenReturn(true);
    Rnr requisition = make(a(RequisitionBuilder.defaultRequisition));
    when(requisitionService.getFullRequisitionById(orderId)).thenReturn(requisition);
    doNothing().when(orderPOD).fillPOD(requisition);
    when(repository.insert(orderPOD)).thenReturn(orderPOD);

    OrderPOD pod = podService.createPOD(orderPOD);

    verify(orderPOD).fillPOD(requisition);
    verify(orderPOD).fillPODLineItems(requisition.getAllLineItems());
    verify(podService).checkPermissions(orderPOD);
    verify(repository).insert(orderPOD);
    assertThat(pod, is(orderPOD));
  }

  @Test
  public void shouldGetPODByOrderId() {
    Long orderId = 2l;
    OrderPOD expectedOrderPOD = new OrderPOD();
    when(repository.getPODByOrderId(orderId)).thenReturn(expectedOrderPOD);

    OrderPOD savedOrderPOD = podService.getPODByOrderId(orderId);

    verify(repository).getPODByOrderId(orderId);
    assertThat(savedOrderPOD, is(expectedOrderPOD));
  }

  @Test
  public void shouldGetPODWithLineItemsById() throws Exception {
    podService.getPodById(podId);
    verify(repository).getPOD(podId);
  }

  @Test
  public void shouldThrowErrorIfUserDoesNotHavePermissionOnGivenWareHouse() {
    orderPod.setModifiedBy(7L);
    when(fulfillmentPermissionService.hasPermission(7L, orderId, MANAGE_POD)).thenReturn(false);

    expectedException.expect(DataException.class);
    expectedException.expectMessage("error.permission.denied");

    podService.checkPermissions(orderPod);
  }

  @Test
  public void shouldNotThrowErrorIfUserHasPermissionOnGivenWareHouse() {
    orderPod.setModifiedBy(7L);
    when(fulfillmentPermissionService.hasPermission(7L, orderId, MANAGE_POD)).thenReturn(true);

    podService.checkPermissions(orderPod);
  }

  @Test
  public void shouldSavePODWithModifiedLineItems() throws ParseException {
    OrderPOD existingPOD = mock(OrderPOD.class);
    when(repository.getPOD(podId)).thenReturn(existingPOD);
    when(existingPOD.getModifiedBy()).thenReturn(userId);
    when(existingPOD.getOrderId()).thenReturn(orderId);
    doNothing().when(podService).checkPermissions(existingPOD);
    when(repository.update(existingPOD)).thenReturn(existingPOD);

    OrderPOD savedPod = podService.save(orderPod);

    verify(repository).update(savedPod);
    verify(existingPOD).copy(orderPod);
  }

  @Test
  public void shouldNotSaveOrderPodIfUserDoesNotHavePermissions() throws ParseException {
    OrderPOD existingPOD = mock(OrderPOD.class);
    when(repository.getPOD(podId)).thenReturn(existingPOD);
    when(existingPOD.getModifiedBy()).thenReturn(userId);
    when(existingPOD.getOrderId()).thenReturn(orderId);
    doThrow(new DataException("error.permission.denied")).when(podService).checkPermissions(existingPOD);

    expectedException.expect(DataException.class);
    expectedException.expectMessage("error.permission.denied");

    podService.save(orderPod);
  }

  @Test
  public void shouldNotSaveAnAlreadySubmittedPOD() throws Exception {
    OrderPOD existingPOD = mock(OrderPOD.class);
    when(repository.getPOD(podId)).thenReturn(existingPOD);
    when(existingPOD.getModifiedBy()).thenReturn(userId);
    when(existingPOD.getOrderId()).thenReturn(orderId);
    doNothing().when(podService).checkPermissions(existingPOD);
    when(orderService.hasStatus(orderId, RECEIVED)).thenReturn(true);

    expectedException.expect(DataException.class);
    expectedException.expectMessage("error.pod.already.submitted");

    podService.save(orderPod);
  }

  @Test
  public void shouldNotSubmitPODIfUserDoesNotHaveTheRight() throws Exception {
    OrderPOD orderPOD = new OrderPOD();
    orderPOD.setId(1234L);
    when(repository.getPOD(1234L)).thenReturn(orderPOD);
    doThrow(new DataException("error.permission.denied")).when(podService).checkPermissions(orderPOD);

    expectedException.expect(DataException.class);
    expectedException.expectMessage("error.permission.denied");

    podService.submit(1234L, userId);

    verify(podService).checkPermissions(orderPOD);
  }

  @Test
  public void shouldValidatePODLineItems() throws Exception {
    long podId = 1234L;
    OrderPOD orderPOD = mock(OrderPOD.class);
    long orderId = 2345L;
    when(orderPOD.getOrderId()).thenReturn(orderId);
    when(orderService.hasStatus(orderId, OrderStatus.RECEIVED)).thenReturn(false);
    when(repository.getPOD(podId)).thenReturn(orderPOD);
    doNothing().when(podService).checkPermissions(orderPOD);
    doThrow(new DataException("error.invalid.received.quantity")).when(orderPOD).validate();

    expectedException.expect(DataException.class);
    expectedException.expectMessage("error.invalid.received.quantity");

    podService.submit(podId, userId);
  }

  @Test
  public void shouldChangeOrderStatusWhenPODIsSubmittedSuccessfully() throws Exception {
    long podId = 1234L;
    OrderPOD orderPOD = mock(OrderPOD.class);
    long orderId = 2345L;
    when(orderPOD.getOrderId()).thenReturn(orderId);
    when(orderService.hasStatus(orderId, OrderStatus.RECEIVED)).thenReturn(false);
    when(repository.getPOD(podId)).thenReturn(orderPOD);
    doNothing().when(podService).checkPermissions(orderPOD);
    doNothing().when(orderPOD).validate();

    podService.submit(podId, userId);

    verify(orderService).updateOrderStatus(new Order(3L, RECEIVED));
  }

  @Test
  public void shouldReturnSubmittedPOD() throws Exception {
    long podId = 1234L;
    long orderId = 2345L;

    OrderPOD orderPOD = mock(OrderPOD.class);
    when(orderPOD.getOrderId()).thenReturn(orderId);
    when(orderService.hasStatus(orderId, OrderStatus.RECEIVED)).thenReturn(false);

    when(repository.getPOD(podId)).thenReturn(orderPOD);
    doNothing().when(podService).checkPermissions(orderPOD);
    doNothing().when(orderPOD).validate();
    when(repository.update(orderPOD)).thenReturn(orderPOD);

    OrderPOD submittedPOD = podService.submit(podId, userId);

    assertThat(submittedPOD, is(orderPOD));
  }

  @Test
  public void shouldNotSubmitAlreadySubmittedPOD() throws Exception {
    long podId = 1234L;
    long orderId = 2345L;
    OrderPOD orderPOD = new OrderPOD();
    orderPOD.setOrderId(orderId);
    when(repository.getPOD(podId)).thenReturn(orderPOD);
    doNothing().when(podService).checkPermissions(orderPOD);
    when(orderService.hasStatus(orderId, OrderStatus.RECEIVED)).thenReturn(true);

    expectedException.expect(DataException.class);
    expectedException.expectMessage("error.pod.already.submitted");

    podService.submit(podId, 5432L);
  }
}
