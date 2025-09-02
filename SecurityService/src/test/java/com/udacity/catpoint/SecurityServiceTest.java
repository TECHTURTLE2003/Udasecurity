package com.udacity.catpoint;


import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.catpoint.service.FakeImageService;
import com.udacity.catpoint.service.ImageService;
import com.udacity.catpoint.services.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit test for simple SecurityService.
 */

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest
{
    /**
     * Rigorous Test :-)
     */
    private SecurityService securityservice;
    private Sensor sensor;
    private final String random = UUID.randomUUID().toString();
    @Mock
    private ImageService imageservice;
    @Mock
    private SecurityRepository securityrepository;

    private Sensor getNewSensorDetails() {
        return new Sensor(random, SensorType.DOOR);
    }

   private Set<Sensor> getAllSensorsDetails(int count, boolean status) {
       return IntStream.range(0, count)
               .mapToObj(i -> {
                   Sensor sensor = new Sensor(random, SensorType.DOOR);
                   sensor.setActive(status);
                   return sensor;
               })
               .collect(Collectors.toSet());
   }

    @BeforeEach
    void init() {
     securityservice = new SecurityService(securityrepository,imageservice);
     sensor = getNewSensorDetails();
    }
    //Test 1
    @Test
    void givenAlarmIsArmedAndSensorIsActive_changeStatusToPendingState() {

        given(securityrepository.getArmingStatus()).willReturn(ArmingStatus.ARMED_HOME);
        given(securityrepository.getAlarmStatus()).willReturn(AlarmStatus.NO_ALARM);
        securityservice.changeSensorActivationStatus(sensor, true);
        then(securityrepository).should().setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test 2
    @Test
    void givenAlarmIsArmedAndSensorIsActive_whenPendingAlarm_thenSetStatusToAlarm() {

        given(securityrepository.getArmingStatus()).willReturn(ArmingStatus.ARMED_HOME);
        given(securityrepository.getAlarmStatus()).willReturn(AlarmStatus.PENDING_ALARM);
        securityservice.changeSensorActivationStatus(sensor, true);
        then(securityrepository).should().setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 3
    @Test
    void givenAlarmIsPendingAndSensorIsInactive_returnNoAlarmState() {

        given(securityrepository.getAlarmStatus()).willReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityservice.changeSensorActivationStatus(sensor);
        then(securityrepository).should().setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test 4
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldNotChangeAlarmStatus_whenAlarmAlreadyActive_andSensorStateChanges(boolean status) {

        given(securityrepository.getAlarmStatus()).willReturn(AlarmStatus.ALARM);
        securityservice.changeSensorActivationStatus(sensor, status);
        then(securityrepository).should(never()).setAlarmStatus(any());
    }
    //Test 5
    @Test
    void givenPendingAlarm_whenSensorActivatedAgain_thenAlarmIsTriggered() {

        given(securityrepository.getAlarmStatus()).willReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityservice.changeSensorActivationStatus(sensor, true);
        then(securityrepository).should().setAlarmStatus(AlarmStatus.ALARM);
    }
    //Test 6
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void shouldNotChangeAlarm_whenInactiveSensorIsDeactivated(AlarmStatus status) {

        given(securityrepository.getAlarmStatus()).willReturn(status);
        sensor.setActive(false);
        securityservice.changeSensorActivationStatus(sensor, false);
        then(securityrepository).should(never()).setAlarmStatus(any());
    }

    //Test 7
    @Test
    void givenArmedHome_whenCatDetected_thenSetAlarmStatusToAlarm() {

        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        given(securityrepository.getArmingStatus()).willReturn(ArmingStatus.ARMED_HOME);
        given(imageservice.imageContainsCat(any(), anyFloat())).willReturn(true);
        securityservice.processImage(catImage);
        then(securityrepository).should().setAlarmStatus(AlarmStatus.ALARM);
    }



    //Test 8
    @Test
    void givenNoCatDetectedAndAllSensorsInactive_thenSetStatusToNoAlarm() {

        Set<Sensor> sensors = getAllSensorsDetails(3, false);
        lenient().when(securityrepository.getSensors()).thenReturn(sensors);
        lenient().when(imageservice.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityservice.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityrepository).setAlarmStatus(captor.capture());
        assertEquals(AlarmStatus.NO_ALARM, captor.getValue());
    }
    //Test 9
    @Test
    void shouldSetNoAlarm_whenSystemIsDisarmed() {

        securityservice.setArmingStatus(ArmingStatus.DISARMED);
        then(securityrepository).should().setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //Test 10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void shouldResetAllSensorsToInactive_whenSystemIsArmed(ArmingStatus status) {

        Set<Sensor> sensors = getAllSensorsDetails(3, true);
        given(securityrepository.getSensors()).willReturn(sensors);
        given(securityrepository.getAlarmStatus()).willReturn(AlarmStatus.PENDING_ALARM);
        securityservice.setArmingStatus(status);
        assertTrue(securityservice.getSensors().stream().allMatch(s -> !s.getActive()));
    }
    //Test 11
    @Test
    void shouldSetAlarm_whenSystemArmedHomeWhileCatDetected() {

        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        given(imageservice.imageContainsCat(any(), anyFloat())).willReturn(true);
        given(securityrepository.getArmingStatus()).willReturn(ArmingStatus.DISARMED);
        securityservice.processImage(catImage);
        securityservice.setArmingStatus(ArmingStatus.ARMED_HOME);
        then(securityrepository).should().setAlarmStatus(AlarmStatus.ALARM);
    }


}
